#!/bin/bash
docker stop --time 60 spring 2>/dev/null || true
docker rm spring 2>/dev/null || true
exit 0
