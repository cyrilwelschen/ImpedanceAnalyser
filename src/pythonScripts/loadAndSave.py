# coding: utf-8

import sys
import os
import numpy as np
import csv

def test_format(filename):
    with open(filename, 'r') as f:
        reader = csv.reader(f, delimiter=',')
        row1_column = next(reader)
        if len(row1_column) == 1:
            if row1_column[0][:3] == "#Di":
                return ",", "dig"
            elif row1_column[0][:3] == "[He":
                return ";", "kur"
            else:
                return ";", "vna"
        else:
            return ",", "vna"

def load_waveform_measurement_dic(filename):
    delimiter_to_use, device_code = test_format(filename)
    with open(filename, 'r') as f:
        reader = csv.reader(f, delimiter=delimiter_to_use)
        data_as_list = list(reader)
    if device_code == "vna":
        col_names = data_as_list[0]
        clean_data = []
        for row in data_as_list[1:]:
            clean_row = []
            for row_item in row:
                try:
                    clean_row.append(float(row_item.replace(",", ".")))
                except ValueError:
                    clean_row.append(0)
            if sum(clean_row) == 0:
                continue
            else:
                clean_data.append(clean_row)
        d = np.array(clean_data, dtype=float).T
    elif device_code == "dig":
        col_names = data_as_list[5]
        d = np.array(data_as_list[6:], dtype=float).T
    elif device_code == "kur":
        col_names = ["Frequency(Hz)", "|Z|", "Phase(deg)"]
        clean_data = []
        start_to_save = False
        for row in data_as_list[1:]:
            clean_row = []
            for row_item in row:
                if row_item == "X Wert":
                    start_to_save = True
                if start_to_save:
                    try:
                        clean_row.append(float(row_item))
                    except ValueError:
                        continue
            if len(clean_row) == 2:
                clean_data.append(clean_row+[0])
        d = np.array(clean_data, dtype=float).T

    this_measure = {}
    for i, c in enumerate(col_names):
        this_measure[c] = d[i]
    return this_measure

def impedance_from_waveform(relative_path_to_file):
    f = 'Frequency (Hz)'
    ph = 'Trace ? ()'
    mag = 'Trace |Z| (O)'
    v = load_waveform_measurement_dic(relative_path_to_file)
    dic_keys = v.keys()
    if mag in dic_keys:
        for k in dic_keys:
            if k[:-2] == 'Trace ? (':
                ph = k
        try:
            return v[f], v[mag], v[ph]
        except KeyError:
            #TODO: give back a message that informes about the keyerror
            return [], [], []
    else:
        #file = open('C:/Users/taawecy2/Desktop/analyser_log.txt', 'a')
        #for d in dic_keys:
        #    file.write(d)
        #file.write("\n")
        #file.close()
        try:
            return v["Frequency(Hz)"], v["|Z|"], v["Phase(deg)"]
        except KeyError:
            #TODO: give back a message that informes about the keyerror
            return [], [], []

def ifft(f, mag, ph, cable_velocity=0.725):
    df = (f[-1] - f[0])/len(f)
    y1 = np.array(list(mag*np.exp(1j*np.array(ph)/180*np.pi)))
    y2 = list(mag*np.exp(-1j*np.array(ph)/180*np.pi))
    y_mirror = []
    for i, val in enumerate(y2):
        y_mirror.append(y2[-(i+1)])
    y = np.array(list(y1) + y_mirror)
    return cable_velocity*3*10**8*1/(2*len(y)*df)*np.arange(len(y)), np.fft.ifft(y).real


p = sys.argv[1]
dir_n = os.path.dirname(p)
file_n = os.path.basename(p)
hiden_calculated = dir_n +"/HIDDEN_CALCULATED_IMPDIST_"+file_n
hiden_calculated_freq = dir_n +"/HIDDEN_CALCULATED_IMPFREQ_"+file_n
if not os.path.exists(hiden_calculated):
    freqs, z_abs, z_phase = impedance_from_waveform(p)
    x, y = ifft(freqs, z_abs, z_phase)
    with open(hiden_calculated, 'w', newline='') as csvfile:
        spamwriter = csv.writer(csvfile, delimiter=',')
        for xi, yi in zip(x, y):
            spamwriter.writerow([xi, yi])
    with open(hiden_calculated_freq, 'w', newline='') as csvfile:
        spamwriter = csv.writer(csvfile, delimiter=',')
        for xi, yi in zip(freqs, z_abs):
            spamwriter.writerow([xi, yi])
    os.system("attrib +h {}".format(hiden_calculated.replace("/", "\\")))
    os.system("attrib +h {}".format(hiden_calculated_freq.replace("/", "\\")))


