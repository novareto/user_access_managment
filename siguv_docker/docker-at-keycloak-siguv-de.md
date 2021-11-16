# Installation Keycloak on Docker on keycloak.siguv.de

## Preparation

    sudo -i
    apt-get update
    sudo apt-get install ca-certificates curl gnupg lsb-release

## Install Docker from Packages

see https://docs.docker.com/engine/install/debian/#install-from-a-package

    export http_proxy=http://10.33.176.21:3128
    export https_proxy=http://10.33.176.21:3128
    
    curl -O https://download.docker.com/linux/debian/dists/buster/pool/stable/amd64/containerd.io_1.4.9-1_amd64.deb
    curl -O https://download.docker.com/linux/debian/dists/buster/pool/stable/amd64/docker-ce-cli_20.10.9~3-0~debian-buster_amd64.deb
    curl -O https://download.docker.com/linux/debian/dists/buster/pool/stable/amd64/docker-ce_20.10.9~3-0~debian-buster_amd64.deb
    dpkg -i containerd.io_1.4.9-1_amd64.deb
    dpkg -i docker-ce-cli_20.10.9~3-0~debian-buster_amd64.deb
    dpkg -i docker-ce_20.10.9~3-0~debian-buster_amd64.deb
    docker --version

## Configure Proxy for Docker

see https://docs.docker.com/config/daemon/systemd/

    mkdir -p /etc/systemd/system/docker.service.d
    
    [Service]
    Environment="HTTP_PROXY=http://10.33.176.21:3128"
    Environment="HTTPS_PROXY=http://10.33.176.21:3128"
    
    sudo systemctl daemon-reload
    sudo systemctl restart docker
    
    docker run hello-world

# Postinstall Steps (add user to group, start on boot)

see https://docs.docker.com/engine/install/linux-postinstall/

    usermod -aG docker $USER
    
    systemctl enable docker.service
    systemctl enable containerd.service

# Install Docker-Compose

see https://docs.docker.com/compose/install/

    curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    
    docker-compose --version
