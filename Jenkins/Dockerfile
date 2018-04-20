FROM jenkins
MAINTAINER Hares Faten  <faten.hrs1994@gmail.com>

# Suppress apt installation warnings
ENV DEBIAN_FRONTEND=noninteractive

# Change to root user
USER root

# Used to set the docker group ID
# Set to 497 by default, which is the group ID used by AWS Linux ECS Instance
ARG DOCKER_GID=497

# Create Docker Group with GID
# Set default value of 497 if DOCKER_GID set to blank string by Docker Compose
RUN groupadd -g ${DOCKER_GID:-497} docker

# Used to control Docker and Docker Compose versions installed
# NOTE: As of February 2016, AWS Linux ECS only supports Docker 1.9.1
#ARG DOCKER_ENGINE=1.10.2
#ARG DOCKER_COMPOSE=1.6.2

# Install base packages
RUN apt-get update -y && \
    apt-get install apt-transport-https curl python-dev python-setuptools gcc make libssl-dev -y && \
    easy_install pip


############################################################################################################
############################################################################################################

# Install Docker Engine
RUN  apt-get update &&\
      apt-get install --no-install-recommends apt-transport-https &&\
   #   curl software-properties-common &&\
   curl -fsSL 'https://sks-keyservers.net/pks/lookup?op=get&search=0xee6d536cf7dc86e2d7d56f59a178ac6c6238f52e' |  apt-key add - &&\
    echo "deb https://packages.docker.com/1.12/apt/repo/ ubuntu-xenial main" | tee /etc/apt/sources.list.d/docker.list && \
   # add-apt-repository "deb https://packages.docker.com/1.12/apt/repo/ ubuntu-xenial main" &&\
    apt-get update &&\
    apt-get -y install docker-engine &&\
    usermod -aG docker jenkins && \
    usermod -aG users jenkins


############################################################################################################
############################################################################################################
# Install Docker Compose
RUN pip install docker-compose && \
    pip install ansible boto boto3

##############################################################################################################
#############################################################################################################
 # Install  base packages 
RUN apt-get install maven --assume-yes &&\
    apt-get install pv &&\
    apt-get install ssh --assume-yes &&\
    apt-get install sshpass

# Add Jenkins plugins
COPY plugins.txt /usr/share/jenkins/plugins.txt
RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt
