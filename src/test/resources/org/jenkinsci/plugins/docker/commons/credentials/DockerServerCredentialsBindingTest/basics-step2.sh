#!/bin/sh
set -e -x

# get path of the certificate directory
cert_path=$(cat cert-path)

# check it was where we would expect it to be
echo "$cert_path" | grep -q '/workspace/p@tmp/secretFiles/[-0-9a-f]\{36\}$'

# check it has been deleted
if [ -e "$cert_path" ] ; then
  echo "$cert_path still exists!!!" >&2
  exit 1
fi
