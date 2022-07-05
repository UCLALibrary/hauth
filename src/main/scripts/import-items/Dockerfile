FROM python:3

# Install app

WORKDIR /usr/src/app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY *.py .

# Run app

SHELL [ "/bin/bash", "-c" ]
RUN shopt -s globstar

ARG CSV_VOLUME_MOUNT_POINT
WORKDIR $CSV_VOLUME_MOUNT_POINT

CMD python /usr/src/app/import-items.py --api-key $HAUTH_API_KEY $HAUTH_BASE_URL {.,**}/*.csv
