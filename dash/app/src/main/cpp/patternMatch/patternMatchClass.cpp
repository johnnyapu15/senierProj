#include "patternMatchClass.h"

#define OUTLAYER "ip2"  //Define out-layer name of Neural Network 
#define IMGSIZE 32      //Define image size used in NN. (Square img)
#define TIMEVALUE 0.1	//The time dimension is on sec/10.
#define DEG2RAD 0.0174533
#define MARGIN 7
#define CLASSNUM 7

patternMatch::patternMatch() {
	//net = new Net();
	//pointVec = new vector<Point2d>;
}
patternMatch::~patternMatch() {
	if (net != NULL)
		delete(net);
	if (pointVec != NULL)
		delete(pointVec);
	if (confidence != NULL)
		delete(confidence);
}

//Initialize canvas & parameters
void patternMatch::initParam2Img(long time){
	if (confidence != NULL)
        delete confidence;
	confidence = new float[CLASSNUM];
    if (pointVec != NULL)
        delete pointVec;
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
	pointVec->push_back(prePoint);
}

//Initialize CNN for pattern classifier
//Use path of caffemodel, prototxt file
void patternMatch::initPatternNN(string path){
	if (this->net != NULL)
		delete this->net;
    pModel = path + "/pattern.caffemodel";
    pProto = path + "/pattern.prototxt";
    
	try {
		this->net = new Net(dnn::readNetFromCaffe((const string)pProto, (const string)pModel));
	}
	catch (cv::Exception& e) {
		std::cerr << "Exception: " << e.what() << std::endl;
		if (this->net->empty())
		{
			std::cerr << "Can't load network by using the following files: " << std::endl;
			std::cerr << "prototxt:   " << pProto << std::endl;
			std::cerr << "caffemodel: " << pModel << std::endl;
			exit(-1);
		}
	}
}

//Draw line from previous-point to present-point.
//Decide the present point with interval from previous function call and paramater v, a.
//the time-input-value is on second/10.
void patternMatch::updateParam2Img(long time, float v, float a){
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
            pointVec->push_back(presPoint);

            //preT = time;
            preT++;

            //This deviding (/10) make time-dimension to sec/10.
            preAngle += TIMEVALUE * angleD;
            prePoint = presPoint;
        }
        preVelo = v;
        angleD = a * DEG2RAD;
        //free(presPoint);
    }
}

//20171129 Johnnyapu15
//Get confidence using CNN-LeNet and calc two-top label.
	
void patternMatch::getTwoTop(int first, int second, float threshold){

	int idx = -1;
	int idx2 = -1;
	float tmpConf = -1;

	//Get predicted confidence
	//this->getPredicted(conf);

	//Find maximum item
	for (int i = 0; i < CLASSNUM; i++){
		if (tmpConf < confidence[i]) {
			idx = i;
			tmpConf = confidence[i];
		}
	}
	if (!(tmpConf > threshold))
		first = idx;
	else first = -1;
	tmpConf = -1;

	//Find 2nd item
	for (int i = 0; i < CLASSNUM; i++){
		if (tmpConf < confidence[i])
			if (idx != i) {
				idx2 = i;
				tmpConf = confidence[i];
			}
	}
	if (!(tmpConf > threshold))
		second = idx2;
	else second = -1;
}

void patternMatch::getThreeTop(int &first, int &second, int &third, float threshold) {

	int idx = -1;
	int idx2 = -1;
	int idx3 = -1;
	float tmpConf = -1;

	//Get predicted confidence
	//this->getPredicted(conf);

	//Find maximum item
	for (int i = 0; i < CLASSNUM; i++) {
		if (tmpConf < confidence[i]) {
			idx = i;
			tmpConf = confidence[i];
		}
	}
	if ((tmpConf > threshold))
		first = idx;
	else first = -1;
	tmpConf = -1;

	//Find 2nd item
	for (int i = 0; i < CLASSNUM; i++) {
		if (tmpConf < confidence[i]) {
			if (idx != i) {
				idx2 = i;
				tmpConf = confidence[i];
			}
		}
	}
	if ((tmpConf > threshold))
		second = idx2;
	else second = -1;
	tmpConf = -1;
	//Find 3rd item
	for (int i = 0; i < CLASSNUM; i++) {
		if (tmpConf < confidence[i]) {
			if (idx != i && idx2 != i) {
				idx3 = i;
				tmpConf = confidence[i];
			}
		}
	}
	if ((tmpConf > threshold))
		third = idx3;
	else third = -1;
}

bool patternMatch::isValid(int idx, float threshold) {
	bool ret = false;
	
	ret = (confidence[idx] > threshold);
	
	return ret;
}

bool patternMatch::isTop(int idx) {
	bool ret = false;
	float maxC = 0;
	for (int i = 0; i < CLASSNUM; i++)
		if (maxC < confidence[i])
			maxC = confidence[i];
	ret = (confidence[idx] == maxC);

	return ret;
}

void patternMatch::getImageFromParam(Mat& img, bool isFullSize){
	if (&img != NULL) {

        if (isFullSize == true) {
            img = Scalar(255, 255, 255);
            for (vector<Point2d>::iterator it = pointVec->begin();
                 it != pointVec->end() - 1;
                 it++) {
                line(img, *(it), *(it + 1), Scalar(0),
                     int(((maxPoint.x - minPoint.x) / 30 + (maxPoint.y - minPoint.y) / 30) / 2) + 1);
            }
        }
        else {

            Mat* canvas = new Mat(Size(400, 400), CV_8UC3);
            *canvas = Scalar(255, 255, 255);
            for (vector<Point2d>::iterator it = pointVec->begin();
                 it != pointVec->end() - 1;
                 it++) {
                line(*canvas, *(it), *(it + 1), Scalar(0),
                     int(((maxPoint.x - minPoint.x) / 30 + (maxPoint.y - minPoint.y) / 30) / 2) + 1);

            }
            ((*canvas)(Rect(minPoint, maxPoint))).copyTo(img);
            delete canvas;
        }

	}
}

void patternMatch::getPredicted(string method, float* confs){
    Mat tmp;
    Mat prob;
	this->getImageFromParam(tmp, 0);
        

    if (tmp.empty())
    {
        std::cerr << "Can't read image" << std::endl;
        exit(-1);
    }
	
	if (method.compare("CNN") == 0) {
		//Image prep.
		Mat inputBlob = blobFromImage(tmp, 1.0f, Size(IMGSIZE, IMGSIZE),
			0, false);


		//CV_TRACE_REGION("forward");
		//Set the network input
		this->net->setInput(inputBlob, "data");

		//Compute output using CNN
		prob = this->net->forward(OUTLAYER);
	}




	//GET CONFIDENCES
	if (prob.empty()) {
		std::cerr << "Can't use method: " << method << std::endl;
		exit(-1);
	}
	else {
		//cout << prob;
		for (int i = 0; i < CLASSNUM; i++)
			this->confidence[i] = ((float*)prob.data)[i];

		if (confs != NULL)
			for (int i = 0; i < CLASSNUM; i++)
				confs[i] = ((float*)prob.data)[i];
	}

}

int patternMatch::getPointNum(){
	return this->pointVec->size();
}

