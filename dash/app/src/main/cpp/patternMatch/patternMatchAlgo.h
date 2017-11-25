#pragma once
//Pattern matching source using CNN LeNet.

/*

classes:
0 = CIRCLE
1 = N
2 = RECT
3 = STAR
4 = L
5 = S
6 = RL
7 = RS

*/

#ifndef patternMatchAlgo

#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include "useDNN.h"


void setParam(String pm, String pp);

void initPatternNN();

void cmp(int det, float conf, int& preDet, float& preConf);

void forwardPatternNN(Mat& img, float* confidences);

#endif // !patternMatch


