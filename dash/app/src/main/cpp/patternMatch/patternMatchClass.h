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

	float* confidence;
    int preT;
    float preVelo;
    float preAngle;
    float angleD;
    Point2d prePoint, minPoint, maxPoint;
    vector<Point2d>* pointVec = NULL;

public:

    patternMatch();
    ~patternMatch();

	//(In the legacy code: patternMatch)
	//Functions of initiation and update paramter, getting predicted are used within JNI.
	void initParam2Img(int time);
	void initPatternNN(string path);
	void updateParam2Img(int time, float v, float a);
	void getPredicted(string method, float* confs);
	void getTwoTop(int first, int second, float threshold);
	void getThreeTop(int &first, int &second, int &third, float threshold);
	void getImageFromParam(Mat& img);
	bool isValid(int idx, float threshold);
	Net* net = NULL;
};

//Initialize canvas & parame
#endif // !patternMatchClass
