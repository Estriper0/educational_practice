from pathlib import Path
import copy, importlib.util, json, subprocess, tempfile, sys
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('procedural',ROOT/'КТ2/src/main.py')
py=importlib.util.module_from_spec(spec);spec.loader.exec_module(py)
original=json.loads((ROOT/'КТ2/src/example.json').read_text(encoding='utf-8'))
java=r'C:\Program Files\Java\jdk-24\bin\java.exe'
cases=[]
def case(name,change=lambda d:None,batch='B1'):
    d=copy.deepcopy(original);change(d);cases.append((name,d,batch))
case('original')
case('out_of_range',lambda d:d['measurements'][0].update(value=20))
case('missing',lambda d:d['measurements'].pop())
case('duplicate_pair',lambda d:d['measurements'].append(dict(d['measurements'][0],id='M5')))
case('wrong_unit',lambda d:d['measurements'][0].update(unit='V'))
for v in [0,10,-0.0001,10.0001,True,False,None,'5',{},[],1e300]:
    case('value_'+str(v),lambda d,v=v:d['measurements'][0].update(value=v))
for table in original:
    case('no_table_'+table,lambda d,t=table:d.pop(t))
    case('not_list_'+table,lambda d,t=table:d.update({t:{}}))
for table in original:
    if original[table] and table!='plan_requirements':
        case('duplicate_id_'+table,lambda d,t=table:d[t].append(copy.deepcopy(d[t][0])))
case('duplicate_rule',lambda d:d['plan_requirements'].append(dict(d['plan_requirements'][0])))
case('reversed_bounds',lambda d:d['plan_requirements'][0].update(lower=50))
case('boolean_bound',lambda d:d['plan_requirements'][0].update(lower=True))
case('no_samples',lambda d:(d.update(samples=[],measurements=[])))
case('no_rules',lambda d:(d.update(plan_requirements=[],measurements=[])))
case('bad_sample_fk',lambda d:d['measurements'][0].update(sample_id='missing'))
case('bad_plan_fk',lambda d:d['batches'][0].update(plan_id='missing'))
case('missing_batch',batch='unknown')
def approval(d,decision='accepted',employee='E2'):
    d['protocols']=[dict(id='PR1',batch_id='B1',employee_id=employee,decision=decision,approved_at='2026-11-24T12:00:00+03:00')]
case('approved',approval)
case('unauthorized',lambda d:approval(d,employee='E1'))
case('contradiction',lambda d:approval(d,decision='rejected'))
case('rejected_approved',lambda d:(d['measurements'][0].update(value=20),approval(d,decision='rejected')))
case('incomplete_approved',lambda d:(d['measurements'].pop(),approval(d)))
case('empty_approval_date',lambda d:(approval(d),d['protocols'][0].update(approved_at='')))
case('duplicate_protocol',lambda d:(approval(d),d['protocols'].append(dict(d['protocols'][0],id='PR2'))))
results=[]
with tempfile.TemporaryDirectory() as tmp:
    path=Path(tmp)/'data.json'
    for name,d,batch in cases:
        try: expected=py.make_protocol(d,batch); invalid=False
        except (ValueError,TypeError,KeyError): expected=None;invalid=True
        path.write_text(json.dumps(d,ensure_ascii=False),encoding='utf-8')
        proc=subprocess.run([java,'-cp',str(ROOT/'materials/kt3/build'),'Main',str(path),'--batch',batch],capture_output=True,encoding='utf-8')
        assert proc.returncode==(2 if invalid else 0),(name,proc.stderr)
        if not invalid: assert json.loads(proc.stdout)==expected,(name,proc.stdout,expected)
        results.append(dict(scenario=name,result='PASS',expected_error=invalid))
(ROOT/'materials/kt3/equivalence_results.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
print(f'PASS: {len(results)} differential scenarios; all JSON fields and issue order match')
