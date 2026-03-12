import os
from datetime import datetime

from flask import Flask, jsonify, redirect, render_template, request, url_for
from flask_sqlalchemy import SQLAlchemy

app = Flask(__name__)

db_path = os.path.join(os.path.abspath(os.path.dirname(__file__)), "bill.db")
app.config["SQLALCHEMY_DATABASE_URI"] = f"sqlite:///{db_path}"
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
app.config["SECRET_KEY"] = "bill-secret-key-dev"

db = SQLAlchemy(app)


class Category(db.Model):
    __tablename__ = "categories"
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), nullable=False, unique=True)
    color = db.Column(db.String(7), nullable=False, default="#6366f1")
    icon = db.Column(db.String(50), nullable=False, default="receipt_long")
    costs = db.relationship("Cost", backref="category", lazy=True, cascade="all, delete-orphan")

    def total(self):
        return sum(c.value for c in self.costs)


class Cost(db.Model):
    __tablename__ = "costs"
    id = db.Column(db.Integer, primary_key=True)
    value = db.Column(db.Float, nullable=False)
    comment = db.Column(db.String(255), nullable=True, default="")
    category_id = db.Column(db.Integer, db.ForeignKey("categories.id"), nullable=False)
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


with app.app_context():
    db.create_all()
    seed_categories()


@app.route("/")
def index():
    categories = Category.query.order_by(Category.name).all()
    return render_template("index.html", categories=categories)


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

    cost = Cost(value=value, category_id=int(category_id), comment=comment)
    db.session.add(cost)
    db.session.commit()

    return redirect(url_for("index"))


@app.route("/dashboard")
def dashboard():
    categories = Category.query.order_by(Category.name).all()
    grand_total = sum(cat.total() for cat in categories)
    return render_template("dashboard.html", categories=categories, grand_total=grand_total)


@app.route("/costs/<int:cost_id>/edit", methods=["GET"])
def edit_cost_page(cost_id):
    cost = Cost.query.get_or_404(cost_id)
    categories = Category.query.order_by(Category.name).all()
    return render_template("edit.html", cost=cost, categories=categories)


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
            db.session.commit()
        except ValueError:
            pass

    return redirect(url_for("dashboard"))


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
