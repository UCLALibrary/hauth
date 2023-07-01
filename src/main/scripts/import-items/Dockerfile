FROM python:3

# Install the executable

WORKDIR /usr/local/bin
COPY docker-entrypoint.sh import-items.py requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
RUN chmod +x docker-entrypoint.sh import-items.py

SHELL [ "/bin/bash", "-c" ]
ENTRYPOINT [ "/usr/local/bin/docker-entrypoint.sh" ]
