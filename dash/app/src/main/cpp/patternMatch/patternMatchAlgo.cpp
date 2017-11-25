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

#include "patternMatchAlgo.h"

String pModel = "/data/data/Dash/files/pattern.caffemodel";
String pProto = "/data/data/Dash/files/pattern.prototxt";
String outLayer = "ip2";
Size imgSize;
Net* net = new Net();


void setParam(String pm, String pp) {
	pModel = pm;
	pProto = pp;
}

void initPatternNN() {
	initNN(pModel, pProto, *net);
	imgSize.width = 32;
	imgSize.height = 32;
}


void cmp(int det, float conf, int& preDet, float& preConf) {
	if (preConf < conf) {
		preDet = det;
		preConf = conf;
	}
}

void forwardPatternNN(Mat& img, float* confidences) {
	Mat prob;
	forwardNN(*net, outLayer, img, imgSize, 0, prob);
	cout << prob;
    for (int i = 0;i<6; i++)
        confidences[i] = ((float*)prob.data)[i];
}
