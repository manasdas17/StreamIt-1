#!/usr/bin/python

import getopt
import os
import string
import sys

# Useful globals:
streamit = os.environ['STREAMIT_HOME']
libdir = os.path.join(streamit, 'library/c')

class RegTest:
    def __init__(self):
        self.directory = ""
        self.output = ""
        self.sources = []

    def setDir(self, dir):
        self.directory = dir

    def setOutput(self, out):
        self.output = out

    def addSource(self, src):
        self.sources.append(src)

    def runCommand(self, cmd):
        print cmd
        return os.system(cmd)

    def report(self, msg):
        print
        print "*** " + msg
        print
    
    def test(self):
        oldwd = os.getcwd()
        os.chdir(os.path.join(streamit, self.directory))
        result = self.dotest()
        os.chdir(oldwd)
        return result

    def dotest(self):
        result = self.runCommand("java -classic at.dms.kjc.Main -s " +
                                 string.join(self.sources) + " > reg-out.c")
        if (result != 0):
            self.report("StreamIt compilation failed")
            return result

        result = self.runCommand("gcc -o reg-out -I" + libdir +
                                 " -L" + libdir + " reg-out.c -lstreamit")
        if (result != 0):
            self.report("gcc compilation failed")
            return result
        
        return result
    
class RegTestSet:
    def __init__(self):
        self.tests = {}

    def add(self, name, test):
        self.tests[name] = test
    
    def limit(self, names):
        set = RegTestSet()
        for name in names:
            set.add(name, self.tests[name])
        return set

    def report(self, msg):
        print
        print ">>> " + msg
        print

    def run_tests(self):
        for name in self.tests.keys():
            self.report("Testing " + name)
            self.tests[name].test()

class ControlReader:
    class ParseError:
        pass

    wantTest, haveTest, wantOpen, wantDecl, haveDir, haveOutput, haveSource = range(7)
    
    def __init__(self):
        self.state = self.wantTest
        self.set = RegTestSet()
    
    def read_control(self, file):
        f = open(file, 'r')
        while 1:
            line = f.readline()
            if line == "":
                break
            # TODO: drop comments
            for word in string.split(line):
                self.read_word(word)
        f.close()
        return self.set

    def read_word(self, word):
        if self.state == self.wantTest:
            if word == "test":
                self.state = self.haveTest
                self.test = RegTest()
            else:
                raise self.ParseError()
        elif self.state == self.haveTest:
            self.testname = word
            self.state = self.wantOpen
        elif self.state == self.wantOpen:
            if word == "{":
                self.state = self.wantDecl
            else:
                raise self.ParseError()
        elif self.state == self.wantDecl:
            if word == "directory":
                self.state = self.haveDir
            elif word == "output":
                self.state = self.haveOutput
            elif word == "source":
                self.state = self.haveSource
            elif word == "}":
                self.set.add(self.testname, self.test)
                self.state = self.wantTest
            else:
                raise self.ParseError()
        elif self.state == self.haveDir:
            self.test.setDir(word)
            self.state = self.wantDecl
        elif self.state == self.haveOutput:
            self.test.setOutput(word)
            self.state = self.wantDecl
        elif self.state == self.haveSource:
            self.test.addSource(word)
            self.state = self.wantDecl
        else:
            raise self.ParseError()

class Options:
    def __init__(self):
        self.checkout = 0
        self.build = 0
        self.test = 1
        self.control = 'control'
        self.cases = []

    def get_options(self, args):
        optlist, args = getopt.getopt(args, '',
                                      ['checkout', 'nocheckout',
                                       'build', 'nobuild',
                                       'test', 'notest',
                                       'control=', 'case='])
        for (opt, val) in optlist:
            if opt == '--nocheckout':
                self.checkout = 0
            if opt == '--checkout':
                self.checkout = 1
            if opt == '--nobuild':
                self.build = 0
            if opt == '--build':
                self.build = 1
            if opt == '--notest':
                self.test = 0
            if opt == '--test':
                self.test = 1
            if opt == '--control':
                self.control = val
            if opt == '--case':
                self.cases.append(val)
        return args

opts = Options()
args = opts.get_options(sys.argv[1:])
set = ControlReader().read_control(opts.control)
if opts.checkout:
    raise NotImplementedError("Checkout not supported yet")
if opts.cases != []:
    set = set.limit(opts.cases)
if opts.build:
    raise NotImplementedError("Build not supported yet")
if opts.test:
    set.run_tests()

