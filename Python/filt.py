#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sun Jun 20 01:21:15 2021

@author: n7
"""

import numpy as np

butter_50 = {1: [-1, 1], 2: [-1, -2, -1]}
butter_10 = {1: [2.078, 4.078], 2: [-6.12, 16.944, 14.825]}
butter_5 = {1: [5.314, 7.314], 2: [-31.934, 77.727, 49.792]}
butter_3 = {1: [9.579, 11.579], 2: [-97.952, 221.826, 127.874]}
butter_2 = {1: [14.895, 16.895], 2: [-231.158, 503.273, 276.115]}
butter_1 = {1: [30.821, 32.821], 2: [-968.544, 2023.090, 1058.546]}
butter_half = {1: [62.657, 64.657], 2: [-3963.156, 8102.361, 4143.205]}

butter_coeffs = {50: butter_50, 10: butter_10, 5: butter_5, 3: butter_3,
                 2: butter_2, 1: butter_1, 0.5: butter_half}


class Butter():
    def __init__(self, order=1, freq=0.5):
        """
        Object for applying Butterworth filter to a variable.

        Parameters
        ----------
        order : int, optional
            Order of the filter. The default is 1.
        freq : float, optional
            Cut-off Frequency (in Hz). The default is 0.5.

        Returns
        -------
        None.

        :Authors:
            Nishad Mandlik <n.g.mandlik@student.tudelft.nl>

        """
        if (freq not in butter_coeffs.keys()):
            raise ValueError("Filter Coeficients Not Available For " +
                             str(freq) + "Hz Frequency")
        if (order not in butter_coeffs[freq].keys()):
            raise ValueError("Order " + str(order) + " Coeficients Not "
                             "Available For " + str(freq) + "Hz Frequency")

        self.freq = freq
        self.order = order
        self.coeffs = butter_coeffs[freq][order]
        self.arr = np.zeros((2, order+1))
        self.started = False

    def _filt1(self, val):
        """
        Return the first-order filtered value of the sample.

        Parameters
        ----------
        val : float
            Sample amplitude.

        Returns
        -------
        None.

        :Authors:
            Nishad Mandlik <n.g.mandlik@student.tudelft.nl>

        """
        if (not self.started):
            self. started = True
            self.arr[0, 1] = val
            self.arr[1, 1] = val
        self.arr[0, 0] = self.arr[0, 1]
        self.arr[0, 1] = val
        self.arr[1, 0] = self.arr[1, 1]
        self.arr[1, 1] = ((self.arr[0, 0] + self.arr[0, 1]) +
                          (self.coeffs[0] * self.arr[1, 0]))/self.coeffs[1]
        return self.arr[1, 1]

    def _filt2(self, val):
        """
        Return the second-order filtered value of the sample.

        Parameters
        ----------
        val : float
            Sample amplitude.

        Returns
        -------
        None.

        :Authors:
            Nishad Mandlik <n.g.mandlik@student.tudelft.nl>

        """
        if (not self.started):
            self. started = True
            self.arr[0, 1] = val
            self.arr[0, 2] = val
            self.arr[1, 1] = val
            self.arr[1, 2] = val
        self.arr[0, 0] = self.arr[0, 1]
        self.arr[0, 1] = self.arr[0, 2]
        self.arr[0, 2] = val
        self.arr[1, 0] = self.arr[1, 1]
        self.arr[1, 1] = self.arr[1, 2]
        self.arr[1, 2] = ((self.arr[0, 0] + 2*self.arr[0, 1] +
                           self.arr[0, 2]) +
                          (self.coeffs[0] * self.arr[1, 0]) +
                          (self.coeffs[1] * self.arr[1, 1]))/self.coeffs[2]
        return self.arr[1, 2]

    def filt(self, val):
        """
        Return the filtered value of the sample.

        Parameters
        ----------
        val : float
            Sample amplitude.

        Returns
        -------
        None.

        :Authors:
            Nishad Mandlik <n.g.mandlik@student.tudelft.nl>

        """
        if (self.order == 1):
            return self._filt1(val)
        elif (self.order == 2):
            return self._filt2(val)
        else:
            return 0
