# -*- coding: utf-8 -*-
"""
Spyder Editor

This is a temporary script file.
"""

import json
import pandas as pd
from pyltx import table as tab

JSON_FILE_PATH = "temp.json"

rooms = ["Living Room", "Kitchen", "Toilet", "Bedroom1", "Bedroom2", "Balcony"]
motions = ["Sitting", "Walking", "Push-Ups"]

# sort = rooms
sort = motions

with open(JSON_FILE_PATH) as f:
    confusion_json = f.read()

confusion_json = confusion_json.replace(",}" , "}")
confusion_dict = json.loads(confusion_json)

df = pd.DataFrame.from_dict(confusion_dict, "index").fillna(0)
df = df.reindex(columns=sort, index=sort)

ltx = tab.tab(df, caption="Confusion Matrix", ref="cm", index=True)
print(ltx)
