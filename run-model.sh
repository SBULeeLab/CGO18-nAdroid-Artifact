APKNAME=$1

JARFlag=$2
if [ "$JARFlag" == "true" ]; then
    echo "Searching android.jar in Sdk/platforms/!"
	ANDROID_JAR=$PWD"/AndroidJar/platforms/"
else
	echo "Using selected android.jar!"
	ANDROID_JAR=$PWD"/AndroidJar/android.jar"
fi

MODEL_JAR_DIR=$PWD"/ModelJar"
MODEL_JAR="Model.jar"
MODEL_OUTPUT_DIR=$PWD"/APKModeling/"

APK="$(find $MODEL_OUTPUT_DIR -iname $APKNAME)"
APKLOCATION="$(dirname $APK)"

rm -rf $APKLOCATION/classes
rm -rf $APKLOCATION/file3.txt
rm -rf $APKLOCATION/componentCallbacksMap
rm -rf $APKLOCATION/componentCallbacksMapReadable
rm -rf $APKLOCATION/componentHandleMethodMap
mkdir $APKLOCATION/classes

echo $APK
echo $ANDROID_JAR

cd $MODEL_JAR_DIR
java -jar $MODEL_JAR $APK $ANDROID_JAR
cd ..
