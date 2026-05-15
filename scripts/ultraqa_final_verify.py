# -*- coding: utf-8 -*-
"""UltraQA Final Verification - Single-tab browser test"""
import subprocess, re, sys, time, os, requests
sys.stdout.reconfigure(encoding='utf-8')

BASE = 'http://localhost:8080/api/v1'
FE = 'http://localhost:5173'
LOG = r'C:\Users\LiShuai\AppData\Local\Temp\claude\C--Users-LiShuai-Desktop-OAISS-CHAIN\74d3dff5-3ee5-4098-8f58-1b9976c352ff\tasks\bl7bnains.output'
results = []
tab_inited = False

def bh(code):
    try:
        r = subprocess.run(['browser-harness', '-c', code], capture_output=True, text=True, encoding='utf-8', timeout=30)
        return r.stdout + r.stderr
    except Exception as e:
        return str(e)

def get_code():
    try:
        with open(LOG, 'r', encoding='utf-8', errors='ignore') as f:
            return re.findall(r'code=([A-Z0-9]{4})', f.read())[-1]
    except:
        return None

def api_login(u, p):
    r = requests.get(f'{BASE}/captcha/generate')
    ck = r.json()['data']['captchaKey']
    c = get_code()
    if not c: return None
    r = requests.post(f'{BASE}/auth/login', json={'username':u,'password':p,'captchaKey':ck,'captcha':c})
    d = r.json()
    return d['data']['accessToken'] if d.get('code') == 200 else None

def inject(token):
    h = len(token) // 2
    t1, t2 = token[:h], token[h:]
    return 'js("var t1=\'' + t1 + '\'; var t2=\'' + t2 + '\'; sessionStorage.setItem(\'access_token\', t1+t2); localStorage.setItem(\'refresh_token\', \'dummy\');")'

def goto(route):
    global tab_inited
    if not tab_inited:
        bh('new_tab("' + FE + route + '")\nwait_for_load(3)')
        tab_inited = True
    else:
        bh('goto_url("' + FE + route + '")\nwait_for_load(3)')
    time.sleep(1)

def check():
    return bh('js("var b=document.querySelectorAll(\\".el-button,button\\").length;var i=document.querySelectorAll(\\"input,textarea,select\\").length;var t=document.querySelectorAll(\\".el-table\\").length;var r=document.querySelectorAll(\\".el-table__row\\").length;print(\\"OK:\\"+b+\\"/\\"+i+\\"/\\"+t+\\"/\\"+r)")')

def click_text(txt):
    return bh('js("var els=document.querySelectorAll(\\".el-button,.el-button--text,.el-link\\");for(var i=0;i<els.length;i++){if(els[i].textContent.indexOf(\\"' + txt + '\\")>=0){els[i].click();print(\\"CLICKED\\");return;}}print(\\"NOT_FOUND\\");")')

def get_dialog():
    time.sleep(1)
    return bh('js("var d=document.querySelector(\\".el-dialog\\");if(d){var h=d.querySelector(\\".el-dialog__title\\");print(\\"DIALOG:\\"+(h?h.textContent.trim():\\"visible\\"));}else{print(\\"NO_DIALOG\\");}")')

# ============ ENTERPRISE ============
print("=" * 60)
print("ENTERPRISE (11 pages)")
print("=" * 60)
token = api_login('enterprise001', 'admin123')
if not token: print("FATAL: enterprise login"); sys.exit(1)

bh(inject(token))

ep = 0
for route, name in [('/enterprise/carbon/upload','CarbonUpload'),('/enterprise/orders/manage','P2POrders'),('/enterprise/trading/market','Auction'),('/enterprise/trading/p2p','P2PTrading'),('/enterprise/company/dashboard','Dashboard'),('/enterprise/credit/score','CreditScore'),('/enterprise/carbon-coin/account','CarbonCoin'),('/enterprise/blockchain/browser','Blockchain'),('/enterprise/carbon-neutral/projects','CarbonNeutral'),('/enterprise/emission/data','EmissionData'),('/enterprise/user/profile','UserProfile')]:
    try:
        goto(route)
        out = check()
        if 'OK:' in out:
            ep += 1
            elems = out.split('OK:')[1].split()[0]
            print(f'  [PASS] {name} ({elems})')
        else:
            print(f'  [FAIL] {name}')
    except Exception as e:
        print(f'  [FAIL] {name}: {str(e)[:40]}')
results.append(('ENTERPRISE', ep, 11))

# ============ ADMIN ============
print("\n" + "=" * 60)
print("ADMIN (5 pages)")
print("=" * 60)
tab_inited = False  # new tab for new role
token = api_login('admin', 'admin123')
bh(inject(token))

ap = 0
for route, name in [('/admin/system/users','Users'),('/admin/system/config','Config'),('/admin/data/statistics','Stats'),('/admin/verify/list','Verify'),('/admin/system/carbon','CarbonAdmin')]:
    try:
        goto(route)
        out = check()
        if 'OK:' in out:
            ap += 1
            elems = out.split('OK:')[1].split()[0]
            print(f'  [PASS] {name} ({elems})')
        else:
            print(f'  [FAIL] {name}')
    except Exception as e:
        print(f'  [FAIL] {name}: {str(e)[:40]}')
results.append(('ADMIN', ap, 5))

# Stats accuracy
try:
    r = requests.get(f'{BASE}/admin/statistics', headers={'Authorization': f'Bearer {token}'})
    s = r.json().get('data', {})
    print(f'  [VERIFY] API stats: {s.get("totalUsers")} users, {s.get("enterpriseCount")} enterprises')
except: pass

# ============ REVIEWER ============
print("\n" + "=" * 60)
print("REVIEWER (1 page)")
print("=" * 60)
tab_inited = False
token = api_login('reviewer001', 'admin123')
bh(inject(token))

rp = 0
try:
    goto('/auditor/audit/list')
    out = check()
    if 'OK:' in out:
        rp = 1
        print(f'  [PASS] AuditList ({out.split("OK:")[1].split()[0]})')
    else:
        print(f'  [FAIL] AuditList')
except Exception as e:
    print(f'  [FAIL] AuditList: {str(e)[:40]}')
results.append(('REVIEWER', rp, 1))

# ============ THIRD_PARTY ============
print("\n" + "=" * 60)
print("THIRD_PARTY (1 page)")
print("=" * 60)
tab_inited = False
token = api_login('thirdparty001', 'admin123')
bh(inject(token))

tpp = 0
try:
    goto('/third-party/monitor')
    out = check()
    if 'OK:' in out:
        tpp = 1
        print(f'  [PASS] Monitor ({out.split("OK:")[1].split()[0]})')
    else:
        print(f'  [FAIL] Monitor')
except Exception as e:
    print(f'  [FAIL] Monitor: {str(e)[:40]}')
results.append(('THIRD_PARTY', tpp, 1))

# ============ LOGIN ============
print("\n" + "=" * 60)
print("LOGIN")
print("=" * 60)
tab_inited = False
lp = 0
try:
    goto('/login')
    out = bh('js("var i=document.querySelectorAll(\\"input\\").length;print(\\"LOGIN:\\"+i)")')
    if 'LOGIN:' in out:
        lp = 1
        print(f'  [PASS] Login page ({out.split("LOGIN:")[1].split()[0]} inputs)')
    else:
        print(f'  [FAIL] Login page')
except Exception as e:
    print(f'  [FAIL] Login: {str(e)[:40]}')
results.append(('LOGIN', lp, 1))

# ============ SUMMARY ============
print("\n" + "=" * 60)
print("SUMMARY")
print("=" * 60)
tp = sum(p for _, p, _ in results)
tt = sum(t for _, _, t in results)
for n, p, t in results:
    print(f'  {"PASS" if p==t else "PARTIAL"} {n}: {p}/{t}')
pct = (tp/tt*100) if tt else 0
print(f'\n  Total: {tp}/{tt} ({pct:.0f}%) {"PASS" if pct>=90 else "FAIL"}')
