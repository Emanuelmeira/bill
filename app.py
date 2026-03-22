import os
from datetime import datetime

from flask import Flask, jsonify, redirect, render_template, request, url_for
from surrealdb import Surreal

try:
    from surrealdb import RecordID
except ImportError:
    class RecordID:
        def __init__(self, table, id):
            self.table_name = table
            self.id = id

        def __str__(self):
            return f"{self.table_name}:{self.id}"

app = Flask(__name__)

SURREAL_URL = os.environ.get("SURREAL_URL", "ws://localhost:8000/rpc")
SURREAL_NS = os.environ.get("SURREAL_NS", "bill")
SURREAL_DB = os.environ.get("SURREAL_DB", "bill")
SURREAL_USER = os.environ.get("SURREAL_USER", "root")
SURREAL_PASS = os.environ.get("SURREAL_PASS", "root")

MONTH_NAMES = [
    "", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
]

DEFAULT_CATEGORIES = [
    {"name": "Celular", "color": "#6366f1", "icon": "smartphone"},
    {"name": "Moradia", "color": "#f59e0b", "icon": "home"},
    {"name": "Compras", "color": "#10b981", "icon": "shopping_cart"},
    {"name": "Aline", "color": "#ec4899", "icon": "person"},
    {"name": "Emanuel", "color": "#3b82f6", "icon": "person"},
    {"name": "Casal", "color": "#f43f5e", "icon": "favorite"},
    {"name": "Custos Extras", "color": "#8b5cf6", "icon": "add_circle"},
    {"name": "Fixos Recorrentes", "color": "#0ea5e9", "icon": "autorenew"},
    {"name": "Saúde", "color": "#14b8a6", "icon": "health_and_safety"},
    {"name": "Theo", "color": "#f97316", "icon": "child_care"},
    {"name": "Uber", "color": "#1f2937", "icon": "local_taxi"},
]


def get_db():
    db = Surreal(SURREAL_URL)
    db.signin({"username": SURREAL_USER, "password": SURREAL_PASS})
    db.use(SURREAL_NS, SURREAL_DB)
    return db


def rid_str(record_id):
    if isinstance(record_id, RecordID):
        return str(record_id.id)
    s = str(record_id)
    if ":" in s:
        return s.split(":", 1)[1]
    return s


def seed_categories():
    db = get_db()
    try:
        existing = db.select("categories")
        if not existing:
            for cat in DEFAULT_CATEGORIES:
                db.create("categories", cat)
    finally:
        db.close()


def next_month_year():
    now = datetime.now()
    m, y = now.month + 1, now.year
    if m > 12:
        m, y = 1, y + 1
    return m, y


seed_categories()


@app.route("/")
def index():
    m, y = next_month_year()
    db = get_db()
    try:
        cats = db.query("SELECT * FROM categories ORDER BY name")
        for cat in cats:
            cat["id_str"] = rid_str(cat["id"])
    finally:
        db.close()

    return render_template(
        "index.html",
        categories=cats,
        current_month=m,
        current_year=y,
        month_names=MONTH_NAMES,
    )


@app.route("/costs/add", methods=["POST"])
def add_cost():
    value_str = request.form.get("value", "").replace(",", ".")
    category_id = request.form.get("category_id", "")
    comment = request.form.get("comment", "").strip()

    if not value_str or not category_id:
        return redirect(url_for("index"))

    try:
        value = float(value_str)
    except ValueError:
        return redirect(url_for("index"))

    now = datetime.now()
    ref = request.form.get("ref_month", "")
    try:
        parts = ref.split("-")
        cost_year, cost_month = int(parts[0]), int(parts[1])
    except (ValueError, IndexError):
        cost_month, cost_year = now.month, now.year

    installments = request.form.get("installments", 1, type=int)
    installments = max(1, min(installments, 48))

    cat_record_id = RecordID("categories", category_id)

    db = get_db()
    try:
        for i in range(installments):
            m = cost_month + i
            y = cost_year
            while m > 12:
                m -= 12
                y += 1

            if installments > 1:
                label = f"{comment} ({i+1}/{installments})" if comment else f"Parcela {i+1}/{installments}"
            else:
                label = comment

            db.create("costs", {
                "value": value,
                "category": cat_record_id,
                "comment": label,
                "month": m,
                "year": y,
                "created_at": now.isoformat(),
            })
    finally:
        db.close()

    return redirect(url_for("index"))


@app.route("/dashboard")
def dashboard():
    def_m, def_y = next_month_year()
    month = request.args.get("month", def_m, type=int)
    year = request.args.get("year", def_y, type=int)

    db = get_db()
    try:
        cats = db.query("SELECT * FROM categories ORDER BY name")

        costs = db.query(
            "SELECT *, category.name as cat_name, category.color as cat_color, "
            "category.icon as cat_icon FROM costs "
            "WHERE month = $month AND year = $year ORDER BY created_at DESC",
            {"month": month, "year": year},
        )

        months_raw = db.query("SELECT month, year FROM costs GROUP BY month, year ORDER BY year DESC, month DESC")
        available_months = [(r["month"], r["year"]) for r in months_raw if r["month"] is not None]
    finally:
        db.close()

    category_map = {}
    for cat in cats:
        cat_id = rid_str(cat["id"])
        category_map[cat_id] = {
            "name": cat["name"],
            "color": cat["color"],
            "icon": cat["icon"],
            "costs": [],
            "total": 0.0,
        }

    for cost in costs:
        cat_ref = cost.get("category")
        cat_id = rid_str(cat_ref) if cat_ref else None
        if cat_id and cat_id in category_map:
            cost["id_str"] = rid_str(cost["id"])
            category_map[cat_id]["costs"].append(cost)
            category_map[cat_id]["total"] += cost.get("value", 0)

    categories_with_costs = [
        {**v, "id": k}
        for k, v in sorted(category_map.items(), key=lambda x: x[1]["name"])
        if v["costs"]
    ]

    grand_total = sum(c["total"] for c in categories_with_costs)

    available_set = {(m, y) for m, y in available_months}
    for pair in [(def_m, def_y), (month, year)]:
        if pair not in available_set:
            available_months.append(pair)
            available_set.add(pair)
    available_months.sort(key=lambda x: (x[1], x[0]), reverse=True)

    prev_m, prev_y = (12, year - 1) if month == 1 else (month - 1, year)
    next_m, next_y = (1, year + 1) if month == 12 else (month + 1, year)

    return render_template(
        "dashboard.html",
        categories=categories_with_costs,
        grand_total=grand_total,
        current_month=month,
        current_year=year,
        prev_month=prev_m,
        prev_year=prev_y,
        next_month=next_m,
        next_year=next_y,
        available_months=available_months,
        month_names=MONTH_NAMES,
    )


@app.route("/costs/<cost_id>/edit", methods=["GET"])
def edit_cost_page(cost_id):
    db = get_db()
    try:
        cost_list = db.select(RecordID("costs", cost_id))
        cats = db.query("SELECT * FROM categories ORDER BY name")
        for cat in cats:
            cat["id_str"] = rid_str(cat["id"])
    finally:
        db.close()

    if not cost_list:
        return redirect(url_for("dashboard"))

    cost = cost_list if isinstance(cost_list, dict) else cost_list[0] if cost_list else None
    if not cost:
        return redirect(url_for("dashboard"))

    cost["id_str"] = rid_str(cost["id"])
    cost["category_id_str"] = rid_str(cost.get("category", ""))

    return render_template("edit.html", cost=cost, categories=cats, month_names=MONTH_NAMES)


@app.route("/costs/<cost_id>/edit", methods=["POST"])
def edit_cost(cost_id):
    value_str = request.form.get("value", "").replace(",", ".")
    category_id = request.form.get("category_id", "")
    comment = request.form.get("comment", "").strip()

    cost_month, cost_year = next_month_year()

    ref = request.form.get("ref_month", "")
    if ref:
        try:
            parts = ref.split("-")
            cost_year = int(parts[0])
            cost_month = int(parts[1])
        except (ValueError, IndexError):
            pass

    db = get_db()
    try:
        if value_str and category_id:
            try:
                db.merge(RecordID("costs", cost_id), {
                    "value": float(value_str),
                    "category": RecordID("categories", category_id),
                    "comment": comment,
                    "month": cost_month,
                    "year": cost_year,
                })
            except ValueError:
                pass
    finally:
        db.close()

    return redirect(url_for("dashboard", month=cost_month, year=cost_year))


@app.route("/costs/<cost_id>/delete", methods=["POST"])
def delete_cost(cost_id):
    db = get_db()
    try:
        db.delete(RecordID("costs", cost_id))
    finally:
        db.close()
    return redirect(url_for("dashboard"))


@app.route("/categories/manage")
def manage_categories():
    db = get_db()
    try:
        cats = db.query("SELECT * FROM categories ORDER BY name")
        all_costs = db.query("SELECT category FROM costs")

        count_map = {}
        for c in all_costs:
            cat_id = rid_str(c.get("category", ""))
            count_map[cat_id] = count_map.get(cat_id, 0) + 1

        for cat in cats:
            cat["id_str"] = rid_str(cat["id"])
            cat["cost_count_val"] = count_map.get(cat["id_str"], 0)
    finally:
        db.close()

    return render_template("categories.html", categories=cats)


@app.route("/categories/add", methods=["POST"])
def add_category():
    name = request.form.get("name", "").strip()
    color = request.form.get("color", "#6366f1")
    icon = request.form.get("icon", "receipt_long")

    if name:
        db = get_db()
        try:
            existing = db.query("SELECT * FROM categories WHERE name = $name", {"name": name})
            if not existing:
                db.create("categories", {"name": name, "color": color, "icon": icon})
        finally:
            db.close()

    return redirect(url_for("manage_categories"))


@app.route("/categories/<cat_id>/delete", methods=["POST"])
def delete_category(cat_id):
    db = get_db()
    try:
        cat_record = RecordID("categories", cat_id)
        db.query("DELETE FROM costs WHERE category = $cat", {"cat": cat_record})
        db.delete(cat_record)
    finally:
        db.close()
    return redirect(url_for("manage_categories"))


@app.route("/api/costs")
def api_costs():
    db = get_db()
    try:
        cats = db.query("SELECT * FROM categories ORDER BY name")
        costs = db.query("SELECT * FROM costs ORDER BY created_at DESC")
    finally:
        db.close()

    category_map = {}
    for cat in cats:
        cat_id = rid_str(cat["id"])
        category_map[cat_id] = {
            "category": cat["name"],
            "color": cat["color"],
            "total": 0.0,
            "costs": [],
        }

    for cost in costs:
        cat_id = rid_str(cost.get("category", ""))
        if cat_id in category_map:
            category_map[cat_id]["costs"].append({
                "id": rid_str(cost["id"]),
                "value": cost["value"],
                "comment": cost.get("comment", ""),
                "month": cost.get("month"),
                "year": cost.get("year"),
            })
            category_map[cat_id]["total"] += cost["value"]

    return jsonify(list(category_map.values()))


if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0")
