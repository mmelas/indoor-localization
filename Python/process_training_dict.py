#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed May 26 11:31:47 2021

@author: n7
"""

import matplotlib.pyplot as plt
from matplotlib.ticker import FormatStrFormatter
import os
import pandas as pd

_SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))


WIFI_DICT_PATH = os.path.join(_SCRIPT_DIR, "Data/wifi.dict")
CELLULAR_DICT_PATH = os.path.join(_SCRIPT_DIR, "Data/cellular.dict")

def plot(df):
    fig, ax = plt.subplots(1, 1)
    for i in df.index:
        y = df.loc[i].to_numpy()
        plt.plot(df.columns, y/y.sum(), label=i)
    plt.legend()
    ax.set_ylabel("PMF", fontsize="14")
    ax.set_xlabel("RSSI", fontsize="14")
    ax.tick_params(axis="both", which="major", labelsize=12)
    ax.yaxis.set_major_formatter(FormatStrFormatter('%.2f'))
    plt.grid(True, which="both")
    plt.subplots_adjust(top=0.93,
                        bottom=0.15,
                        left=0.11,
                        right=0.98,
                        hspace=0.2,
                        wspace=0.2)
    fig.tight_layout()



if (__name__ == "__main__"):
    with open(WIFI_DICT_PATH, "r") as wifi_file:
        exec("wifi_dict = " + wifi_file.read())

    with open(CELLULAR_DICT_PATH, "r") as cellular_file:
        exec("cellular_dict = " + cellular_file.read())

    wifi_tabs = {}
    cellular_tabs = {}

    for k in wifi_dict:
        wifi_tabs[k] = \
            pd.DataFrame.from_dict(wifi_dict[k],
                                   "index").sort_index(0).sort_index(1, ascending=False).fillna(0)

    for k in cellular_dict:
        cellular_tabs[k] = \
            pd.DataFrame.from_dict(cellular_dict[k],
                                   "index").sort_index(0).sort_index(1, ascending=False).fillna(0)

    # cells = ["Cell 7", "Cell 6"]
    # cells = ["Cell 4", "Cell 6"]
    # temp = {k:wifi_tabs[k].filter(items=cells, axis=0) for k in wifi_tabs
    #         if all(cell in wifi_tabs[k].index for cell in cells)}

    # # Similar PMFs
    # siml_mac = "10:7B:44:B6:25:38"
    # siml  = wifi_tabs[siml_mac].filter(items=["Cell 4", "Cell 6"], axis=0)
    # siml = siml.loc[:, (siml != 0).any(axis=0)]

    # # Different PMFs
    # diff_mac = "74:DA:88:8C:24:FF"
    # diff = wifi_tabs[diff_mac].filter(items=["Cell 7", "Cell 6"], axis=0)
    # diff = diff.loc[:, (diff != 0).any(axis=0)]

    # Similar PMFs
    siml_id = "WCDMA_13283681"
    siml  = cellular_tabs[siml_id].filter(items=["Cell 1", "Cell 2"], axis=0)
    siml = siml.loc[:, (siml != 0).any(axis=0)]

    # Different PMFs
    diff_id = "WCDMA_13283681"
    diff = cellular_tabs[diff_id].filter(items=["Cell 4", "Cell 6"], axis=0)
    diff = diff.loc[:, (diff != 0).any(axis=0)]


    plot(siml)
    plt.savefig("similar.svg")
    plot(diff)
    plt.savefig("different.svg")
