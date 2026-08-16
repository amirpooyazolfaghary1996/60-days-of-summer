"""Inline plan.json and exercises.json into web/index.html."""
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

tpl = open(os.path.join(HERE, "app_template.html")).read()
plan = open(os.path.join(ROOT, "app/src/main/assets/plan.json")).read()
lib = open(os.path.join(ROOT, "app/src/main/assets/exercises.json")).read()

out = tpl.replace("__PLAN__", plan).replace("__LIB__", lib)
path = os.path.join(ROOT, "web/index.html")
open(path, "w").write(out)
print(f"{path}  {len(out)//1024} KB")
