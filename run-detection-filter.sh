APKNAME=$1
echo $APKNAME

TestModeFlag=$2
PropNum=$3
OutputDir=$4

MODEL_OUTPUT_DIR=$PWD"/APKModeling/"
APK="$(find $MODEL_OUTPUT_DIR -iname $APKNAME)"
APKLOCATION="$(dirname $APK)"

CHORD_DIR=$PWD"/DetectionFilterJar/chord-src-2.0"
CHORD_JAR="chord.jar"
Output_Template=$PWD"/DetectionFilterJar/Output_Template"

WORK_DIR="$OutputDir/Result_$(basename $APKNAME .apk)"

rm -rf $WORK_DIR

cp -a $Output_Template $WORK_DIR
cp $WORK_DIR/properties/$PropNum.properties $WORK_DIR/chord.properties
 
cp -a $APKLOCATION/classes $WORK_DIR
cp $APKLOCATION/componentCallbacksMap $WORK_DIR
cp $APKLOCATION/componentCallbacksMapReadable $WORK_DIR
cp $APKLOCATION/componentHandleMethodMap $WORK_DIR

SET_HEAP="-Dchord.max.heap=12g -Dchord.bddbddb.max.heap=12g"
SET_WORK_DIR="-Dchord.work.dir=$WORK_DIR"
CHORD_BOOT="chord.project.Boot"
CHORD_KOBJSENS="-Dchord.kobj.k=2 -Dchord.inst.ctxt.kind=co -Dchord.stat.ctxt.kind=cc"

if [ "$TestModeFlag" == "true" ]; then
	echo "Run test!"
	SET_RUN_ANALYSES="-Dchord.run.analyses=test-java"
else
	echo "Run nAdroid!"
	SET_RUN_ANALYSES="-Dchord.run.analyses=drrace-java"
fi

cd $CHORD_DIR
java -cp $CHORD_JAR $SET_HEAP $SET_WORK_DIR $SET_RUN_ANALYSES $CHORD_KOBJSENS $CHORD_BOOT
cd ..
