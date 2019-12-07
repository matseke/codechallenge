from pyscbwrapper import SCB

scb = SCB('sv', 'ME', 'ME0104', 'ME0104D', 'ME0104T4')
#print (scb.info())
#print (scb.get_variables)

regioner = scb.get_variables()['region']
#print (regioner)
scb.set_query(region=regioner,
              tabellinnehåll=["Valdeltagande i riksdagsval, procent"])
#scb.get_query
scb_data = scb.get_data()
#print (scb_data)
scb_valdeltagande = scb_data['data']
#print (scb_valdeltagande)
koder = scb.get_query()['query'][0]['selection']['values']

landic = {}
for i in range(len(koder)):
  landic[koder[i]] = regioner[i]


landata = {}

for kod in landic:
  landata[landic[kod]] = {}
  for i in range(len(scb_valdeltagande)):
    if scb_valdeltagande[i]['key'][0] == kod:
      landata[landic[kod]][scb_valdeltagande[i]['key'][1]] = \
      float(scb_valdeltagande[i]['values'][0])

print (landata)