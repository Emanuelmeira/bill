import os
from datetime import datetime

from flask import Flask, jsonify, redirect, render_template, request, url_for
from flask_migrate import Migrate
from flask_sqlalchemy import SQLAlchemy

app = Flask(__name__)

db_path = os.path.join(os.path.abspath(os.path.dirname(__file__)), "bill.db")
app.config["SQLALCHEMY_DATABASE_URI"] = f"sqlite:///{db_path}"
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
app.config["SECRET_KEY"] = "bill-secret-key-dev"

db = SQLAlchemy(app)
migrate = Migrate(app, db)


class Category(db.Model):
    __tablename__ = "categories"
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False, unique=True)
    color = db.Column(db.String(7), nullable=False, default="#6366f1")
    icon = db.Column(db.String(50), nullable=False, default="receipt_long")
    costs = db.relationship("Cost", backref="category", lazy=True, cascade="all, delete-orphan")

    def total(self, month=None, year=None):
        costs = self.costs
        if month and year:
            costs = [c for c in costs if c.month == month and c.year == year]
        return sum(c.value for c in costs)

    def costs_for_month(self, month, year):
        return [c for c in self.costs if c.month == month and c.year == year]


MONTH_NAMES = [
    "", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
]


class Cost(db.Model):
    __tablename__ = "costs"
    id = db.Column(db.Integer, primary_key=True)
    value = db.Column(db.Float, nullable=False)
    comment = db.Column(db.String(255), nullable=True, default="")
    category_id = db.Column(db.Integer, db.ForeignKey("categories.id"), nullable=False)
    month = db.Column(db.Integer, nullable=False)
    year = db.Column(db.Integer, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


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


def seed_categories():
    if Category.query.count() == 0:
        for cat_data in DEFAULT_CATEGORIES:
            db.session.add(Category(**cat_data))
        db.session.commit()


def next_month_year():
    now = datetime.now()
    m, y = now.month + 1, now.year
    if m > 12:
        m, y = 1, y + 1
    return m, y


with app.app_context():
    from sqlalchemy import inspect
    inspector = inspect(db.engine)
    if inspector.has_table("categories"):
        seed_categories()


@app.route("/")
def index():
    m, y = next_month_year()
    categories = Category.query.order_by(Category.name).all()
    return render_template(
        "index.html",
        categories=categories,
        current_month=m,
        current_year=y,
        month_names=MONTH_NAMES,
    )


@app.route("/costs/add", methods=["POST"])
def add_cost():
    value = request.form.get("value", "").replace(",", ".")
    category_id = request.form.get("category_id")
    comment = request.form.get("comment", "").strip()

    if not value or not category_id:
        return redirect(url_for("index"))

    try:
        value = float(value)
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

        db.session.add(Cost(
            value=value,
            category_id=int(category_id),
            comment=label,
            month=m,
            year=y,
        ))

    db.session.commit()

    return redirect(url_for("index"))


@app.route("/dashboard")
def dashboard():
    def_m, def_y = next_month_year()
    month = request.args.get("month", def_m, type=int)
    year = request.args.get("year", def_y, type=int)

    categories = Category.query.order_by(Category.name).all()
    grand_total = sum(cat.total(month, year) for cat in categories)

    available_months = (
        db.session.query(Cost.month, Cost.year)
        .distinct()
        .order_by(Cost.year.desc(), Cost.month.desc())
        .all()
    )
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
        categories=categories,
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


@app.route("/costs/<int:cost_id>/edit", methods=["GET"])
def edit_cost_page(cost_id):
    cost = Cost.query.get_or_404(cost_id)
    categories = Category.query.order_by(Category.name).all()
    return render_template("edit.html", cost=cost, categories=categories, month_names=MONTH_NAMES)


@app.route("/costs/<int:cost_id>/edit", methods=["POST"])
def edit_cost(cost_id):
    cost = Cost.query.get_or_404(cost_id)

    value = request.form.get("value", "").replace(",", ".")
    category_id = request.form.get("category_id")
    comment = request.form.get("comment", "").strip()

    if value and category_id:
        try:
            cost.value = float(value)
            cost.category_id = int(category_id)
            cost.comment = comment

            ref = request.form.get("ref_month", "")
            if ref:
                parts = ref.split("-")
                cost.year = int(parts[0])
                cost.month = int(parts[1])

            db.session.commit()
        except (ValueError, IndexError):
            pass

    return redirect(url_for("dashboard", month=cost.month, year=cost.year))


@app.route("/costs/<int:cost_id>/delete", methods=["POST"])
def delete_cost(cost_id):
    cost = Cost.query.get_or_404(cost_id)
    db.session.delete(cost)
    db.session.commit()
    return redirect(url_for("dashboard"))


@app.route("/categories/manage")
def manage_categories():
    categories = Category.query.order_by(Category.name).all()
    return render_template("categories.html", categories=categories)


@app.route("/categories/add", methods=["POST"])
def add_category():
    name = request.form.get("name", "").strip()
    color = request.form.get("color", "#6366f1")
    icon = request.form.get("icon", "receipt_long")

    if name:
        existing = Category.query.filter_by(name=name).first()
        if not existing:
            db.session.add(Category(name=name, color=color, icon=icon))
            db.session.commit()

    return redirect(url_for("manage_categories"))


@app.route("/categories/<int:cat_id>/delete", methods=["POST"])
def delete_category(cat_id):
    cat = Category.query.get_or_404(cat_id)
    db.session.delete(cat)
    db.session.commit()
    return redirect(url_for("manage_categories"))


@app.route("/api/costs")
def api_costs():
    categories = Category.query.order_by(Category.name).all()
    result = []
    for cat in categories:
        result.append({
            "category": cat.name,
            "color": cat.color,
            "total": cat.total(),
            "costs": [
                {"id": c.id, "value": c.value, "comment": c.comment, "date": c.created_at.strftime("%d/%m/%Y")}
                for c in cat.costs
            ],
        })
    return jsonify(result)


if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0")
