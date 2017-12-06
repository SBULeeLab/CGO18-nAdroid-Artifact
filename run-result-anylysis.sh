APKNAME=$1
echo $APKNAME

OUTPUTFILE=$2

RESULTDIR=$PWD"/Result"
WORKDIR="$(find $RESULTDIR -iname Result_$APKNAME)"

ResultAnalysisJarDir=$PWD"/ResultAnalysisJar"
ResultAnalysisJar="ResultAnalysis.jar"
DLOGFILENAME="filterAnalysis.dlog"

cp $ResultAnalysisJarDir/$DLOGFILENAME $WORKDIR/chord_output/bddbddb
DLOGFILE=$WORKDIR/chord_output/bddbddb/$DLOGFILENAME

cd $ResultAnalysisJarDir
java -jar $ResultAnalysisJar $APKNAME $DLOGFILE $OUTPUTFILE
cd ..
