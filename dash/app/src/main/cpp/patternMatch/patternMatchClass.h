#pragma once
#ifndef patternMatchClass

//1.Pattern match using CNN or KNN, etc.
//2.Get image from control parameter of DASH.
#include <vector>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/dnn.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/core/utils/trace.hpp>
#include <iostream>
#include <math.h>
#include "patternMatchClass.h"
using namespace cv;
using namespace cv::dnn;
using namespace std;

class patternMatch {
private:
    //(In the legacy code: patternMatchAlgo)
    string pModel = "/pattern.caffemodel";
    string pProto = "/pattern.prototxt";
   


    //(In the legacy code: useDNN)
    //void initNN(String modelBin, String modelTxt, Net& net);
    //void forwardNN(Net& net, String outLayer, Mat image, Size size, Scalar mean, Mat& prob);
	//String[6] classes = {"CIRCLE", "N", "L", "RECT", "RS", "S", "INTERMEDIATE"};
	float* confidence = NULL;
    long preT;
    float preVelo;
    float preAngle;
    float angleD;
    Point2d prePoint, minPoint, maxPoint;


public:

    patternMatch();
    ~patternMatch();

	//(In the legacy code: patternMatch)
	//Functions of initiation and update paramter, getting predicted are used within JNI.
	void initParam2Img(long time);
	void initPatternNN(string path);
	void updateParam2Img(long time, float v, float a);
	void getPredicted(string method, float* confs);
	void getTwoTop(int first, int second, float threshold);
	void getThreeTop(int &first, int &second, int &third, float threshold);
	void getImageFromParam(Mat& img, bool isFullSize);
	bool isValid(int idx, float threshold);
	int getPointNum();
	Net* net = NULL;
	vector<Point2d>* pointVec = NULL;
};

//Initialize canvas & parame
#endif // !patternMatchClass
