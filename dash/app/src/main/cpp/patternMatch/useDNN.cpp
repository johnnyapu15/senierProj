//20171110~, Classification using CNN
//INPUT: Mat image, String modelBin, String modelTxt
//OUTPUT: result array of CNN model

#include "useDNN.h"

void initNN(String modelBin, String modelTxt, Net& net) {
	CV_TRACE_FUNCTION();
	Net tmpNet;
	const string bin = modelBin;
	const string proto = modelTxt;
	ifstream openFile1(proto);
	bool t1 = openFile1.is_open();
    ifstream openFile2(bin);
    bool t2 = openFile2.is_open();
	//FILE* file = fopen(bin.c_str(),"r");
	//if (openFile.is_open());
	try {
		tmpNet = dnn::readNetFromCaffe(proto, bin);
	}
	catch (cv::Exception& e) {
		std::cerr << "Exception: " << e.what() << std::endl;
		if (net.empty())
		{
			std::cerr << "Can't load network by using the following files: " << std::endl;
			std::cerr << "prototxt:   " << modelTxt << std::endl;
			std::cerr << "caffemodel: " << modelBin << std::endl;
			exit(-1);
		}
	}
	net = tmpNet;
}


void forwardNN(Net& net, String outLayer, Mat image, Size size, Scalar mean, Mat& prob) {
	
	if (image.empty())
	{
		std::cerr << "Can't read image" << std::endl;
		exit(-1);
	}
	//Image prep.
	Mat inputBlob = blobFromImage(image, 1.0f, size,
		mean, false);
	
	
	//CV_TRACE_REGION("forward");
	net.setInput(inputBlob, "data");        //set the network input

	prob = net.forward(outLayer);                          //compute output
}

