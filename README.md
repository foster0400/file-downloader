#File Downloader
####to let user download files from http(s) / (s)ftp. 
                                                     
##Requirement
###1. java 11 -> make sure your java is java 11
####you can type `java -version` on your command line


###2. Prepare 2 json files :
#### 2.1 uri list -> can be seen at file example/url-list.json
#### 2.2 configuration -> can be seen at file example/configuraiton.json

##How to run it
I have prepare the runnable. you can run it by using this command \
```
java -jar path/to/file-downloader-1.0-SNAPSHOT.jar path/to/url-list.json path/to/configuration.json
```