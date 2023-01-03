package io.github.libxposed.service;

interface IXposedScopeCallback {
    oneway void onScopeRequestPrompted(String packageName) = 1;
    oneway void onScopeRequestApproved(String packageName) = 2;
    oneway void onScopeRequestDenied(String packageName) = 3;
    oneway void onScopeRequestTimeout(String packageName) = 4;
    oneway void onScopeRequestFailed(String packageName, String message) = 5;
}
