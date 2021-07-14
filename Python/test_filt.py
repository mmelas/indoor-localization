#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sun Jun 20 01:24:40 2021

@author: n7
"""

from filt import Butter
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

SAMPLING_PERIOD_MS = 50


# df = pd.read_csv(
#     "Data/walk_accel.csv").drop(columns=["gFx", "gFy", "gFz"])
df = pd.read_csv(
    "Data/2021-06-2002.58.50.csv").drop(columns=["gFx", "gFy", "gFz"])
df.columns = ["t", "a"]

df_t = df["t"].to_numpy()
df_a = df["a"].to_numpy()

num_samples = int(df["t"].max()/(SAMPLING_PERIOD_MS/1000))

t = np.arange(SAMPLING_PERIOD_MS/1000, df["t"].max(), SAMPLING_PERIOD_MS/1000)
a = np.zeros(t.size)


for i in range(0, a.size):
    j = np.where(df_t >= ((i+1)*(SAMPLING_PERIOD_MS/1000)))[0][0]
    a[i] = df_a[j]

b = Butter(order=1, freq=0.5)

a_filt = np.zeros(a.size)
for i in range(0, a_filt.size):
    a_filt[i] = b.filt(a[i])

plt.plot(t, a)
plt.plot(t, a_filt)
