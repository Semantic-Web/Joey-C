#!/usr/bin/python

import sys
import json
import datetime as dt
import numpy as np
import matplotlib.pyplot as plt



# 2015-06-30 00:00:00 (from python, need to use isoformat('T'))
# 2015-07-04T21:20:32-0400  (ISO format from iOS)
YMD_FORMAT_STR = "%Y-%m-%d";
HMS_FORMAT_STR = "%H:%M:%S";
DATE_FORMAT_STR = YMD_FORMAT_STR + "T" + HMS_FORMAT_STR;

def truncateDateStr(datetimeStr):
	""" Truncates the given ISO datetime string to just represent the date."""
	formatStr = YMD_FORMAT_STR;
	dateStr = datetimeStr[0:10];
	return dt.datetime.strptime(dateStr, formatStr).isoformat('T');




def showTable(dateString, samples):
	sortedSamples = sorted(samples, key=lambda s: s["startDate"]);





# docs.python.org/2/library/json.html
if len(sys.argv) < 2:
	print "usage: please provide input file argument.";
	sys.exit(1);


try:
	jsonFile = open(str(sys.argv[1]), "rb");
	print "success";

except IOError as e:
	print "couldn't open data file"
	sys.exit(1);


jsonObj = json.load(jsonFile);
steps = jsonObj["stepCounts"];

# Separate all step counts into lists according to their date.
stepCountsByDate = {};

# Separate the step counts into groups according to date. "YYYY-mm-dd"
for i in range(len(steps)):
	data = steps[i];
	if "startDate" not in data:
		continue;

	dateStr = truncateDateStr(data["startDate"])[0:19];

	if dateStr in stepCountsByDate:
		stepCountsByDate[dateStr].append(data);
	else:
		countsList = [data];
		stepCountsByDate[dateStr] = countsList;



# Data is grouped.
keys = sorted(stepCountsByDate.keys());

# Create the graph. 
plt.figure(figsize=(12, 12));
plt.xticks(range(24), range(24));

for i in range( len( keys ) ):
	# Create a 24 hour list of total steps in each hour.
	totals = [0] * 24;
	dataList = stepCountsByDate[ keys[i] ];

	# Total up the step count for each hour in 24 hour format.
	for s in range( len(dataList) ):
		date = dt.datetime.strptime(dataList[s]["startDate"][0:19], DATE_FORMAT_STR);
		totals[ date.hour ] += int(dataList[s]["count"]);
		

	currentDate = dt.datetime.strptime(keys[i], DATE_FORMAT_STR);
	plt.plot(totals, label=currentDate.strftime("%a %b, %d"));

plt.legend();
plt.show();



