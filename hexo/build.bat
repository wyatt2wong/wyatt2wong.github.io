@echo off
cd /d %~dp0
hexo clean && hexo generate
