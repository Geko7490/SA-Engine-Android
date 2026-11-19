package com.geko7490.saengineandroid;

interface IShellService {
    void destroy() = 16777114;
    String execute(String command) = 1;
    int getUid() = 2;
}
