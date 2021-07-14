#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed May  5 08:06:22 2021

@author: n7
"""

from matplotlib import pyplot as plt
import numpy as np
import pandas as pd

WIN_SZ = 2 # Window size in seconds

def plot_stats(ax, t, y):
    t = t.to_numpy()
    y = y.to_numpy()
    ax.plot(t, y, label="Data")
    # ax.plot(t[1:], (y[1:] - y[:-1])/(t[1:] - t[:-1]), label="dy/dx")
    where = [np.logical_and(t > i-WIN_SZ, t <= i) for i in t]
    ax.plot(t, [y[i].std() for i in where], label="Std.")
    ax.plot(t, [y[i].max()-y[i].min() for i in where], label="Amp.")
    ax.legend()


def accel_plot(acc):
    fig, ax = plt.subplots(1, 1)
    plot_stats(ax, acc.t, acc.a)

def gyro_plot(gyr):
    fig, axs = plt.subplots(3, 1)
    plot_stats(axs[0], gyr.t, gyr.x)
    plot_stats(axs[1], gyr.t, gyr.y)
    plot_stats(axs[2], gyr.t, gyr.z)



# a_s = pd.read_csv("Data/accel_sit.csv")
# a_s.pop("gFx")
# a_s.pop("gFy")
# a_s.pop("gFz")
# a_s.columns = ["t", "a"]
# a_w = pd.read_csv("Data/accel_walk.csv")
# a_w.pop("gFx")
# a_w.pop("gFy")
# a_w.pop("gFz")
# a_w.columns = ["t", "a"]

# g_s = pd.read_csv("Data/gyro_sit.csv")
# g_s.columns = ["t", "x", "y", "z"]
# g_w = pd.read_csv("Data/gyro_walk.csv")
# g_w.columns = ["t", "x", "y", "z"]

# accel_plot(a_s)
# accel_plot(a_w)

# gyro_plot(g_s)
# gyro_plot(g_w)



df_sit_a = pd.read_csv("Data/sit_accel.csv").drop(columns=["gFx", "gFy", "gFz"])
df_sit_a.columns = ["t", "a"]
df_sit_g = pd.read_csv("Data/sit_gyro.csv")
df_sit_g.columns = ["t", "wx", "wy", "wz"]

df_walk_a = pd.read_csv("Data/walk_accel.csv").drop(columns=["gFx", "gFy", "gFz"])
df_walk_a.columns = ["t", "a"]
df_walk_g = pd.read_csv("Data/walk_gyro.csv")
df_walk_g.columns = ["t", "wx", "wy", "wz"]

df_pushup_a = pd.read_csv("Data/pushup_accel.csv").drop(columns=["gFx", "gFy", "gFz"])
df_pushup_a.columns = ["t", "a"]
df_pushup_g = pd.read_csv("Data/pushup_gyro.csv")
df_pushup_g.columns = ["t", "wx", "wy", "wz"]

fig, axs = plt.subplots(4, 1, sharex=True)
# fig.tight_layout()

axs[0].set_ylabel("Resultant Acc.")
axs[0].set_ylim(0.2,1.8)

t = df_sit_a["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[0].plot(t[where]-5, df_sit_a["a"].to_numpy()[where], label="Sitting", color="green")

t = df_walk_a["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[0].plot(t[where]-5, df_walk_a["a"].to_numpy()[where], label="Walking")

t = df_pushup_a["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[0].plot(t[where]-5, df_pushup_a["a"].to_numpy()[where], label="Push-Ups")

axs[0].legend()


axs[1].set_ylabel("Ang. Vel. X")
axs[1].set_ylim(-2.5,2.5)

t = df_sit_g["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[1].plot(t[where]-5, df_sit_g["wx"].to_numpy()[where], label="Sitting", color="green")

t = df_walk_g["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[1].plot(t[where]-5, df_walk_g["wx"].to_numpy()[where], label="Walking")

t = df_pushup_g["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[1].plot(t[where]-5, df_pushup_g["wx"].to_numpy()[where], label="Push-Ups")

axs[1].legend()


axs[2].set_ylabel("Ang. Vel. Y")
axs[2].set_ylim(-2.5,2.5)

t = df_sit_g["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[2].plot(t[where]-5, df_sit_g["wy"].to_numpy()[where], label="Sitting", color="green")

t = df_walk_g["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[2].plot(t[where]-5, df_walk_g["wy"].to_numpy()[where], label="Walking")

t = df_pushup_g["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[2].plot(t[where]-5, df_pushup_g["wy"].to_numpy()[where], label="Push-Ups")

axs[2].legend()


axs[3].set_ylabel("Ang. Vel. Z")
axs[3].set_ylim(-2.5,2.5)

t = df_sit_g["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[3].plot(t[where]-5, df_sit_g["wz"].to_numpy()[where], label="Sitting", color="green")

t = df_walk_g["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[3].plot(t[where]-5, df_walk_g["wz"].to_numpy()[where], label="Walking")

t = df_pushup_g["t"].to_numpy()
where = np.logical_and(t>=5, t<7)
axs[3].plot(t[where]-5, df_pushup_g["wz"].to_numpy()[where], label="Push-Ups")

axs[3].legend()

axs[0].set_xlim(0, 2.7)
