#!/bin/sh
set -e -x

if [ `uname` = 'Darwin' ] ; then
  permission_fmt='-f %Lp'
  size_fmt='-f %z'
else
  permission_fmt='-c %a'
  size_fmt='-c %s'
fi

# check permissions on the credentials dir and its parent
[ $(stat $permission_fmt "$DOCKER_CERT_PATH")    = 700 ]
[ $(stat $permission_fmt "$DOCKER_CERT_PATH"/..) = 700 ]

# check permissions and content of the certificate files
[ $(stat $permission_fmt "$DOCKER_CERT_PATH/key.pem")  = 600 ]
[ $(stat $permission_fmt "$DOCKER_CERT_PATH/cert.pem") = 600 ]
[ $(stat $permission_fmt "$DOCKER_CERT_PATH/ca.pem")   = 600 ]
[ $(stat $size_fmt "$DOCKER_CERT_PATH/key.pem")  = 9 ]
[ $(stat $size_fmt "$DOCKER_CERT_PATH/cert.pem") = 17 ]
[ $(stat $size_fmt "$DOCKER_CERT_PATH/ca.pem")   = 19 ]

# keep location of the certificate dir for the next step
echo "$DOCKER_CERT_PATH" > cert-path
