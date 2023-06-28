FROM python:3

# Copy over the executable

WORKDIR /usr/local/bin
COPY docker-entrypoint.sh .
COPY import-items.py .
RUN chmod +x docker-entrypoint.sh import-items.py

# Install script dependencies

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

SHELL [ "/bin/bash", "-c" ]
ENTRYPOINT [ "/usr/local/bin/docker-entrypoint.sh" ]
