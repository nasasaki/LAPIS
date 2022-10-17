#!/usr/bin/bash
set -e
cd /home/sasaki/git/NEW.LAPIS/app

java -Xmx7g -jar lapis.jar --config lapis-config.yml Lapis --api
