#include <vector>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/dnn.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/core/utils/trace.hpp>
//#include <fstream>
//#include <iostream>
//#include <cstdlib>
#include <math.h>

using namespace cv;
using namespace cv::dnn;
using namespace std;

#define OUTLAYER "ip2"  //Define out-layer name of Neural Network 
#define IMGSIZE 32      //Define image size used in NN. (Square img)
#define TIMEVALUE 0.1	//The time dimension is on sec/10.
#define DEG2RAD 0.0174533
#define MARGIN 7

class patternMatch{
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

    patternMatch(){
        //net = new Net();
        //pointVec = new vector<Point2d>;
    }
    ~patternMatch(){
		if (net != NULL)
			delete(net);
		if (pointVec != NULL)
			delete(pointVec);
    }
}

//Initialize canvas & parameters
void patternMatch::initParam2Img(int time){
    if (pointVec != NULL) delete pointVec;
	pointVec = new vector<Point2d>;
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
}

//Initialize CNN for pattern classifier
//Use path of caffemodel, prototxt file
void patternMatch::initPatternNN(String path){
	if (net != NULL) delete net;
	net = new Net();
    pModel = path + "/pattern.caffemodel";
    pProto = path + "/pattern.prototxt";
    
	try {
		net = dnn::readNetFromCaffe((const)proto, (const)bin);
	}
	catch (cv::Exception& e) {
		std::cerr << "Exception: " << e.what() << std::endl;
		if (net.empty())
		{
			std::cerr << "Can't load network by using the following files: " << std::endl;
			std::cerr << "prototxt:   " << pModel << std::endl;
			std::cerr << "caffemodel: " << pProto << std::endl;
			exit(-1);
		}
	}
}

//Draw line from previous-point to present-point.
//Decide the present point with interval from previous function call and paramater v, a.
//the time-input-value is on second/10.
void patternMatch::updateParam2Img(int time, float v, float a){
    if (pointVec == NULL) {
        std::cerr << "Initialize values first." << std::endl;
        exit(-1);
    }
    else {
        Point2d presPoint;
        while (time - preT > 1) {
            
            //Calculate the distance from pre-Pnt to pres-Pnt.
            float d = TIMEVALUE * preVelo / 2;

            presPoint = Point2d(prePoint.x + d*cos(preAngle), prePoint.y + d*sin(preAngle));

            //Modify minimum width and height.
            if (minPoint.x > presPoint.x - MARGIN) minPoint.x = presPoint.x - MARGIN;
            if (minPoint.y > presPoint.y - MARGIN) minPoint.y = presPoint.y - MARGIN;

            //Modify maximum width and height.
            if (maxPoint.x < presPoint.x + MARGIN) maxPoint.x = presPoint.x + MARGIN;
            if (maxPoint.y < presPoint.y + MARGIN) maxPoint.y = presPoint.y + MARGIN;

            //Append current point to point list.
            pointVec.push_back(presPoint);

            //preT = time;
            preT++;

            //This deviding (/10) make time-dimension to sec/10.
            preAngle += TIMEVALUE * angleD;
            prePoint = presPoint;
        }
        preVelo = v;
        angleD = a * DEG2RAD;
        free(presPoint);
    }
}

//20171129 Johnnyapu15
//Get confidence using CNN-LeNet and calc two-top label.
	
void patternMatch::getTwoTop(int first, int second, float threshold){

	int idx = -1;
	int idx2 = -1;
	float tmpConf = -1;
	float* conf = new float[];

	//Get predicted confidence
	this.getPredicted(conf);

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
	
	delete[] conf;
}
void patternMatch::getImageFromParam(Mat& img){
    Mat canvas = Mat(Size(400, 400), CV_8UC3);
	canvas = Scalar(255, 255, 255);
	for (vector<Point2d>::iterator it = pointVec.begin();
		it != pointVec.end()-1;
		it++) {
		line(canvas, *(it), *(it + 1), Scalar(0),
			int(((maxPoint.x - minPoint.x) / 30 + (maxPoint.y - minPoint.y) / 30) / 2) + 1);
	}
    
    img = canvas(Rect(minPoint, maxPoint));
    free(canvas);
}

void patternMatch::getPredicted(float* confs){
    Mat tmp;
    Mat prob;
	this.getImageFromParam(tmp);
        

    if (tmp.empty())
    {
        std::cerr << "Can't read image" << std::endl;
        exit(-1);
    }
    //Image prep.
    Mat inputBlob = blobFromImage(tmp, 1.0f, Size(IMGSIZE, IMGSIZE),
        0, false);
    
    
    //CV_TRACE_REGION("forward");
    //Set the network input
    this.net.setInput(inputBlob, "data");        

    //Compute output using CNN
    prob = this.net.forward(OUTLAYER);                          


    //cout << prob;
    for (int i = 0; i < 6; i++)
        confs[i] = ((float*)prob.data)[i];
    
    free(tmp);
    free(prob);
}

