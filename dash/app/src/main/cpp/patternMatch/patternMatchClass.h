#pragma once
#ifndef patternMatchClass

//1.Pattern match using CNN or KNN, etc.
//2.Get image from control parameter of DASH.

#include "patternMatchAlgo.h"
#include <math.h>

class patternMatch {
private:
    //(In the legacy code: patternMatchAlgo)
    void setParam(String pm, String pp);
    void initPatternNN();
    void cmp(int det, float conf, int& preDet, float& preConf);
    void forwardPatternNN(Mat& img, float* confidences);

    String pModel = "/pattern.caffemodel";
    String pProto = "/pattern.prototxt";
    Net net = NULL;   


    //(In the legacy code: useDNN)
    void initNN(String modelBin, String modelTxt, Net& net);
    void forwardNN(Net& net, String outLayer, Mat image, Size size, Scalar mean, Mat& prob);


    //(In the legacy code: patternMatch)
    //Functions of initiation and update paramter, getting predicted are used within JNI.
    void initParam2Img(int time);
    void initPatternNN(String path);
    void updateParam2Img(int time, float v, float a);
    void getPredicted(float* confs);
    void getTwoTop(int first, int second, float threshold);
    void getImageFromParam(Mat& img);

    int preT;
    float preVelo;
    float preAngle;
    float angleD;
    Point2d prePoint, minPoint, maxPoint;
    vector<Point2d> pointVec = NULL;

public:

    patternMatch();
    ~patternMatch();
};

//Initialize canvas & parame
#endif // !patternMatchClass
