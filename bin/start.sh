set ff=unix
bin_path=`dirname "$0"`
cd "$bin_path/.."
pt=`pwd`
nohup java -Dfile.encoding=utf-8 -Dloader.path=libs/,conf/ -Xms512M -jar zdh_mock.jar >> mock.log  &