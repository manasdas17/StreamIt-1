#!/usr/bin/env python
import os
import subprocess
import time
import re

class Configs:
    static, dynamic = range(2)

streamit_home = os.environ['STREAMIT_HOME']
strc          = os.path.join(streamit_home, 'strc')

def generate(test, work, ratio):
    op = 'void->void pipeline test {\n';
    op += '    add FileReader<float>(\"../input/floats.in\");\n'
    if test == Configs.static:
        op += '    add FstaticPush();\n'
        op += '    add FstaticPop();\n'
    else:
        op += '    add FdynamicPush();\n'
        op += '    add FdynamicPop();\n'
    op += '    add FileWriter<float>(\"test.out\");\n'
    op += '}\n'
    op += '\n'
    op += 'float->float filter FstaticPush() {\n'
    op += '    work pop 1 push ' + str(int(ratio * work)) + ' {\n'
    op += '        int i;\n'
    op += '        float x;\n'
    op += '        x = pop();\n'
    op += '        for (i = 0; i < ' + str(int(work/2)) + '; i++) {\n'
    op += '            x += i * 3.0 - 1.0;\n'
    op += '        }\n'
    op += '        for (i = 0; i < ' + str(int((work/2) * ratio)) + '; i++) {\n'
    op += '            push(x);\n'
    op += '        }\n'
    op += '    }\n'
    op += '}\n'
    op += '\n'
    op += 'float->float filter FstaticPop() {\n'
    op += '    work pop ' + str(int(ratio * work)) + ' push 1 {\n'
    op += '        int i;\n'
    op += '        float x;\n'
    op += '        x = 1;\n'
    op += '        for (i = 0; i < ' + str(int((work/2) * ratio)) + '; i++) {\n'
    op += '            pop();\n'
    op += '        }\n'
    op += '        for (i = 0; i < ' + str(int(work/2)) + '; i++) {\n'
    op += '            x += i * 3.0 - 1.0;\n'
    op += '        }\n'
    op += '        push(x);\n'
    op += '    }\n'
    op += '}\n'
    op += '\n'
    op += 'float->float filter FdynamicPush() {\n'
    op += '    work pop 1 push * {\n'
    op += '        int i;\n'
    op += '        float x;\n'
    op += '        x = pop();\n'
    op += '        for (i = 0; i < ' + str(int(work/2)) + '; i++) {\n'
    op += '            x += i * 3.0 - 1.0;\n'
    op += '        }\n'
    op += '        for (i = 0; i < ' + str(int((work/2) * ratio)) + '; i++) {\n'
    op += '            push(x);\n'
    op += '        }\n'
    op += '    }\n'
    op += '}\n'
    op += '\n'
    op += 'float->float filter FdynamicPop() {\n'
    op += '    work pop * push 1 {\n'
    op += '        int i;\n'
    op += '        float x;\n'
    op += '        x = 1;\n'
    op += '        for (i = 0; i < ' + str(int((work/2) * ratio)) + '; i++) {\n'
    op += '            pop();\n'
    op += '        }\n'
    op += '        for (i = 0; i < ' + str(int(work/2)) + '; i++) {\n'
    op += '            x += i * 3.0 - 1.0;\n'
    op += '        }\n'
    op += '        push(x);\n'
    op += '    }\n'
    op += '}\n'
    op += '\n'
    print op;
    with open("test.str", 'w') as f:
        f.write(op)      

def compile(test, outputs, ignore, core):
    exe = './smp' + str(core)
    if test == Configs.static:
        cmd = [strc, '--perftest', '--noiter',
               '--outputs', str(outputs), '--preoutputs', str(ignore), '-smp', str(core), 'test.str' ]
    else:
        cmd = [strc, '--perftest', '--noiter', '--nofuse',
               '--outputs', str(outputs), '--preoutputs', str(ignore), '-smp', str(core), 'test.str' ]
    print cmd
    subprocess.call(cmd, stdout=open(os.devnull, "w"), stderr=subprocess.STDOUT)
    assert os.path.exists(exe)


def run_one(core):
    print 'run_one'
    exe = './smp' + str(core)
    results = []
    (stdout, error) = subprocess.Popen([exe], stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
    regex = re.compile('input=(\d+) outputs=(\d+) ignored=(\d+) start=\d+:\d+ end=\d+:\d+ delta=(\d+):(\d+)')
    for m in regex.finditer(stdout):
        results = ([core] + [m.group(1)] + [m.group(2)] + [m.group(3)] + [m.group(4)] + [m.group(5)])       
    return results

def run(core, attempts):
    results = []
    for num in range(attempts):
         result = run_one(core)
         results.append(result)
         print result
   # 1000000000 nanoseconds in 1 second    
    times = map(lambda x:  (long(x[4]) * 1000000000L) + long(x[5]) , results)    
    avg = reduce(lambda x, y: float(x) + float(y), times) / len(times)    
    return avg

def print_all(work, static_results, dynamic_results):
    file = 'costcomm' + str(work) + '.dat'
    with open(file, 'w') as f:
        s = '#%s\t%s\t%s\t%s' % ( 'ratio', 'work', 'static', 'dynamic')
        print s
        f.write(s + '\n')  
        for x, y in zip(static_results, dynamic_results):
            s = '%0.2f\t%d\t%0.2f\t%0.2f' % (x[0], x[1], x[2], y[2])
            print s
            f.write(s + '\n')

def plot(work):
    data = 'costcomm' + str(work) + '.dat'
    output = 'costcomm' + str(work) + '.ps'
    # cmd = "plot \"" + data + "\" u 2:3 w linespoints"
    cmd = "plot \"" + data + "\" u 1:3 t \'static\' w linespoints, \"" + data + "\" u 1:4 t \'dynamic\' w linespoints, \"" + data + "\""    
    with open('./tmp.gnu', 'w') as f:        
        f.write('set terminal postscript\n')
        f.write('set output \"' + output + '\"\n')
        f.write('set title \"Static vs Dynamic, Outputs=' + str(work) + '\n') 
        f.write('set xlabel \"Ratio\"\n')
        f.write('set ylabel \"Nanoseconds\"\n')
        f.write(cmd)
    os.system('gnuplot ./tmp.gnu')

def main():         
    attempts = 3
    ignore = 1000
    outputs = 1000
    ratios = [0.10, 0.25, 0.50, 0.75, 0.90]
    total_work = [10000]
    num_cores = 1

    for work in total_work:
        static_results = []
        dynamic_results = []
        for ratio in ratios:
            for test in [Configs.static, Configs.dynamic]:
                generate(test, work, ratio)
                compile(test, outputs, ignore, num_cores)
                avg =  run(num_cores, attempts)
                print 'avg=' + str(avg)
                if test == Configs.static:
                    static_results.append((ratio, work, avg))
                else:
                    dynamic_results.append((ratio, work, avg))
        print_all(work, static_results, dynamic_results);
        plot(work)
                    
if __name__ == "__main__":
    main()
