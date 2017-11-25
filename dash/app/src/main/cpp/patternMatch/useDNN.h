#pragma once
#ifndef dnnHeader

#include <opencv2/dnn.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/core/utils/trace.hpp>

using namespace cv;
using namespace cv::dnn;

#include <fstream>
#include <iostream>
#include <cstdlib>

using namespace std;

void initNN(String modelBin, String modelTxt, Net& net);

void forwardNN(Net& net, String outLayer, Mat image, Size size, Scalar mean, Mat& prob);



#endif // !dnnHeader

