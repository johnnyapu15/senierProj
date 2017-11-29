//1.Pattern match using CNN or KNN, etc.
//2.Get image from control parameter of DASH.

#include "patternMatch.h"
#include <vector>

#define TIMEVALUE 0.1	//The time dimension is on sec/10.
#define DEG2RAD 0.0174533
#define MARGIN 7

private String[6] classes = {"CIRCLE", "N", "L", "RECT", "RS", "S"};
int preT;
float preVelo;
float preAngle;
float angleD;
Point2d prePoint;
Point2d minPoint, maxPoint;
vector<Point2d> pointVec;



//Functions of initiation and update paramter, getting predicted are used within JNI.
void initParam2Img(int time) {
	minPoint.x = 200 - MARGIN;
	minPoint.y = 200 - MARGIN;
	maxPoint.x = 200 + MARGIN;
	maxPoint.y = 200 + MARGIN;
	preT = time;
	preVelo = 0;
	preAngle = -90 * DEG2RAD;
	angleD = 0;
	prePoint = Point2d(200, 200);
	pointVec.push_back(prePoint);
	//pointVec.push_back(prePoint);
	initPatternNN();
}
void updateParam2Img(int time, float v, float a) {
	//Draw line from previous-point to present-point.
	//Decide the present point with interval from previous function call and paramater v, a.
	//the time-input-value is on second/10.
	while (time - preT > 1) {

		Point2d presPoint;

		//Calculate the distance from pre-Pnt to pres-Pnt.

		//float d = C * (time - preT) * preVelo;
		float d = TIMEVALUE * preVelo / 2;

		
		presPoint = Point2d(prePoint.x + d*cos(preAngle), prePoint.y + d*sin(preAngle));

		//Draw line at the end.
		//line(canvas, prePoint, presPoint, Scalar(0), 3);

		//Set minimum width and height.
		if (minPoint.x > presPoint.x - MARGIN) minPoint.x = presPoint.x - MARGIN;
		if (minPoint.y > presPoint.y - MARGIN) minPoint.y = presPoint.y - MARGIN;

		//Set maximum width and height.
		if (maxPoint.x < presPoint.x + MARGIN) maxPoint.x = presPoint.x + MARGIN;
		if (maxPoint.y < presPoint.y + MARGIN) maxPoint.y = presPoint.y + MARGIN;

		//Set pre-variables
		pointVec.push_back(presPoint);

		//preT = time;
		preT++;

		//This deviding (/10) make time-dimension to sec/10.
		preAngle += TIMEVALUE * angleD;
		prePoint = presPoint;
	}
	preVelo = v;
	angleD = a * DEG2RAD;
	
}
void getPredicted(String predAlgorithm, float* confs) {
	//Draw line first, the 
	Mat tmp;
	getImageFromParam(tmp);

	if (predAlgorithm.compare("CNN") == 0) {
		forwardPatternNN(tmp, confs);
	}

}

void getTwoTop(int first, int second, float threshold){
	//20171129 Johnnyapu15
	//get confidence using CNN-LeNet and calc two-top label.
	
	int idx = -1;
	int idx2 = -1;
	float tmpConf = -1;
	float* conf = new float[];

	//Get predicted confidence
	getPredicted("CNN", conf);

	//Find maximum item
	for (int i = 0; i < 6; i++){
		if (tmpConf < conf[i]) {
			idx = i;
			tmpConf = conf[i];
		}
	}
	if (!(tmpConf > threshold))
		first = idx;
	else first = -1;
	tmpConf = -1;

	//Find 2nd item
	for (int i = 0; i < 6; i++){
		if (tmpConf < conf[i])
			if (idx != i) {
				idx2 = i;
				tmpConf = conf[i];
			}
	}
	if (!(tmpConf > threshold))
		second = idx2;
	else second = -1;
	
	delete conf;
}

void getImageFromParam(Mat& img) {
	Mat canvas = Mat(Size(400, 400), CV_8UC3);
	canvas = Scalar(255, 255, 255);
	Mat tmp;
	for (vector<Point2d>::iterator it = pointVec.begin();
		it != pointVec.end()-1;
		it++) {
		line(canvas, *(it), *(it + 1), Scalar(0),
			int(((maxPoint.x - minPoint.x) / 30 + (maxPoint.y - minPoint.y) / 30) / 2) + 1);
	}

	tmp = canvas(Rect(minPoint, maxPoint));
	img = tmp;
}
