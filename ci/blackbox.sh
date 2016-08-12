#!/bin/bash -ex
echo start
#mail settings
#RCVR="abcmauck@gmail.com,jamesyarrington88@gmail.com"
RCVR="jamesyarrington88@gmail.com"
SUBJ= $BUILDURL

#awm

pushd `dirname $0` > /dev/null
base=$(pwd -P)
popd > /dev/null

[ -z "$space" ] && space=int

bigLatch=0

spaces=${target_domain%.*.*}
if [ "$spaces" = "test"]; then
	spaces="int stage prod"
else if ["$spaces" = ""]; then
	spaces="prod"
fi
	
for space in $spaces; do
	echo $space
	envfile=$base/environments/$space.postman_environment

	echo $envfile

	[ -f $envfile ] || { echo "no tests configured for this environment"; exit 0; }

	cmd="newman -x -e $envfile -c"

	latch=0

	set -e
	
	BODY="Failing Collections:"

	for f in $(ls -1 $base/postman/*postman_collection); do
		echo $f
		filename=$(basename $f)
		#Try the command first.  If it returns an error, latch & e-mail.
		$cmd $f || { latch=1; BODY="${BODY}\n${filename%.*}"; } #append the failing collection to the pending body of the e-mail.
		echo $latch
	done
	
	SUBJ="Failure in $space environment!"

	if [ "$latch" -eq "1" ]; then
		echo -e "${BODY}" | mail -s "$SUBJ" $RCVR
		echo "mail sent!"
		bigLatch=1
	fi
done

#Return an overall error if any collections failed.
exit $bigLatch
#awm	
