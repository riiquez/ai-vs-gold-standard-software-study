#!/usr/bin/env python3
"""Ponto de entrada do JetUML."""

import sys

from jetuml.application.jetuml_facade import JetUmlApplication

if __name__ == "__main__":
    sys.exit(JetUmlApplication.run())