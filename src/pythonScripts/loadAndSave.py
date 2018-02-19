# coding: utf-8

import sys
import os
import numpy as np
import csv


def load_waveform_measurement_dic(filename):
    if "\\" in filename:
        file_to_open =  filename
    else:
        file_to_open = 'files/{}.csv'.format(filename)
    with open(file_to_open, 'r') as f:
        reader = csv.reader(f, delimiter=',')
        data_as_list = list(reader)
    col_names = data_as_list[5]
    try:
        if isinstance(float(col_names[0]), float):
            col_names = data_as_list[0]
            d = np.array(data_as_list[1:], dtype=float).T
    except ValueError:
        d = np.array(data_as_list[6:], dtype=float).T

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


