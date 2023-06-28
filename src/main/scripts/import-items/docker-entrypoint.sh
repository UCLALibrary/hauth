#!/bin/bash

# Handle arguments that use globstar
shopt -s globstar

import-items.py --api-key ${1} ${2} ${3}
