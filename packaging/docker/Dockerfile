#
# USC Node Dockerfile
#

# Pull base image.
FROM ubuntu:18.04

MAINTAINER USC Release <usc@ulord.net>

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update
RUN apt-get install -y --no-install-recommends openjdk-8-jre supervisor systemd software-properties-common
RUN apt-get install libssl1.0.0:amd64 
RUN apt-get install net-tools

RUN mkdir -p /root/usc
RUN cd /root/usc
ADD usc /root/usc
RUN /root/usc/set-config.sh


# Supervisod CONF
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

## MAINNET
EXPOSE 58858 58859 5858 58585 50505 5885 5886 58585/udp 50505/udp 5858/udp

# Define default command.
CMD ["/usr/bin/supervisord"]

