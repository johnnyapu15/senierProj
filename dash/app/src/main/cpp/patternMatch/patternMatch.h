#pragma once
#ifndef patternMatch

//1.Pattern match using CNN or KNN, etc.
//2.Get image from control parameter of DASH.

#include "patternMatchAlgo.h"
#include <math.h>




//Functions of initiation and update paramter, getting predicted are used within JNI.
void initParam2Img(int time);
void initPatternNN(String path);
void updateParam2Img(int time, float v, float a);
void getPredicted(String predAlgorithm, float* confs);
void getTwoTop(int first, int second, float threshold);
void getImageFromParam(Mat& img);

#endif // !patternMatch
