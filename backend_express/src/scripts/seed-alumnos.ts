import supabase from '../config/database';

// Datos de los 158 alumnos
const alumnos1A = [
  { apellidos: 'ABAD VILLANERA', nombres: 'LISETH SAYURI' },
  { apellidos: 'BAZAN NAVARRO', nombres: 'LEONARDO ARTURO' },
  { apellidos: 'CABANILLAS LINAN', nombres: 'ANGEL GABRIEL BEAT' },
  { apellidos: 'CARRILLO VILCHEZ', nombres: 'EDGARDO MIGUEL' },
  { apellidos: 'CERNA VILLAR', nombres: 'AARON DAVI' },
  { apellidos: 'CHAVARRIA ARANDA', nombres: 'JEREMY' },
  { apellidos: 'CONDOR OLIVAS', nombres: 'MACKENZYE ROMINA' },
  { apellidos: 'CUZCANO ESPINOZA', nombres: 'LIZETH VALLOLET' },
  { apellidos: 'ESPINOZA YARLEQUE', nombres: 'JHOSTIN' },
  { apellidos: 'GODOY SORIA', nombres: 'KARLA MERLYN' },
  { apellidos: 'GUTIERREZ MORENO', nombres: 'JOSE EDUARDO' },
  { apellidos: 'HUAMAN CARHUACOTA', nombres: 'ENMANUEL JESUS DANIEL' },
  { apellidos: 'HUERTA ASTONITAS', nombres: 'LEONELA' },
  { apellidos: 'INCA PACCORI', nombres: 'JEREMY OSCAR' },
  { apellidos: 'JUAREZ', nombres: 'RUMAY JULY' },
  { apellidos: 'LOYOLA ESPINOLA', nombres: 'CARLOS NICOLAS' },
  { apellidos: 'MARCOS SANCHEZ', nombres: 'ALEXANDER ANDRE' },
  { apellidos: 'MEZA CCENTE', nombres: 'JORDY ANDERSON' },
  { apellidos: 'MINAN VALVERDE', nombres: 'ANA CAMILA' },
  { apellidos: 'NAVENTA UCANAY', nombres: 'DARIANA STEFANY' },
  { apellidos: 'ORE QUEVEDO', nombres: 'JOSE FERNANDO' },
  { apellidos: 'RAYMUNDO SOTO', nombres: 'ALESSANDRO' },
  { apellidos: 'RIOJA TOLEDO', nombres: 'KAREN MILAGROS' },
  { apellidos: 'RIVERA MALLQUI', nombres: 'JEANPIERO MANUEL' },
  { apellidos: 'ROMERO REYES', nombres: 'MIRKO JOHEL' },
  { apellidos: 'SALAS PANAIFO', nombres: 'LEYSY ESTHER' },
  { apellidos: 'SALGADO PALACIOS', nombres: 'ANDRY RYAN' },
  { apellidos: 'SANTIAGO SULCA', nombres: 'MELANY DARLYN' },
  { apellidos: 'SERON ALCA', nombres: 'RODRIGO CRISTOBAL' },
  { apellidos: 'VASQUEZ CANO', nombres: 'EDINSON VALENTINO' },
  { apellidos: 'VEGA AGUERO', nombres: 'CRISTIANO SANTIAGO' },
  { apellidos: 'VILCHEZ PARAGUAY', nombres: 'NAHIDU SOFIA' }
];

const alumnos1B = [
  { apellidos: 'BARRIOS CORDOBA', nombres: 'KEIWERLYN YORGINA' },
  { apellidos: 'BEJARANO TORRES', nombres: 'ALEJANDRO GIANCARLO' },
  { apellidos: 'CANCHO NORIEGA', nombres: 'SHIRLEY SOFIA' },
  { apellidos: 'CASTANEDA VASQUEZ', nombres: 'CRISTOFER JORGE ALBERTO' },
  { apellidos: 'CHUICA ORDONEZ', nombres: 'CARLOS THIAGO' },
  { apellidos: 'CORAL SALDANA', nombres: 'TIARA MELISSA' },
  { apellidos: 'GERVASIO EVANGELISTA', nombres: 'THIAGO ANTHUAN' },
  { apellidos: 'GOMEZ ARANIBAR', nombres: 'DANAE MILUZKA' },
  { apellidos: 'GUERRERO ARIRAMA', nombres: 'KATERIN LUANA' },
  { apellidos: 'GUZMAN CASTILLO', nombres: 'ANDRE THIAGO' },
  { apellidos: 'HERRERA CLAUDIO', nombres: 'NAOMI JAZMIN' },
  { apellidos: 'JUARES JIMENEZ', nombres: 'RODRIGO BELKAN' },
  { apellidos: 'KANEKO PAITAN', nombres: 'BRITTANY YARITZA' },
  { apellidos: 'MARCA BERROSPI', nombres: 'GABRIEL ADRIEL' },
  { apellidos: 'MAYTA CAPCHA', nombres: 'JORDAN' },
  { apellidos: 'MAYTA CAPCHA', nombres: 'JOSUE' },
  { apellidos: 'MUNANTE HUANCA', nombres: 'VALENTINA' },
  { apellidos: 'PENA BARDALES', nombres: 'JUNIOR VALENTIN' },
  { apellidos: 'PEREZ CHUICA', nombres: 'JAN JACOBO' },
  { apellidos: 'PIZARRO DEL AGUILA', nombres: 'IKER ADRIANO' },
  { apellidos: 'POMA PONCE', nombres: 'NADESKA JHASMIN' },
  { apellidos: 'RIVERO PINO', nombres: 'IAN DANIEL' },
  { apellidos: 'ROMERO ZUBIETA', nombres: 'JOAQUIN ANGEL' },
  { apellidos: 'SANCHEZ MORALES', nombres: 'SARAHY DANIELA' },
  { apellidos: 'SORIA PAGUADA', nombres: 'YARELY XIOMARA' },
  { apellidos: 'TOSCANO CHOCO', nombres: 'KEVIN EFRAIN' },
  { apellidos: 'UMAN VARGAS', nombres: 'ADRIANO' },
  { apellidos: 'VASQUEZ SOBRINO', nombres: 'TERRY ANDRE' },
  { apellidos: 'VEGA CASTRO', nombres: 'ROCIO IVETH' },
  { apellidos: 'VILLALVA GARCIA', nombres: 'GABRIEL JOSETH' },
  { apellidos: 'VILLARREAL FERNANDEZ', nombres: 'GEORGE NICOLAS' },
  { apellidos: 'YACOLCA YALICO', nombres: 'YOHAO SEBASTIAN KAREV' }
];

const alumnos1C = [
  { apellidos: 'ACERO SERNAQUE', nombres: 'LUIS NEYMAR' },
  { apellidos: 'ALARCON FLORES', nombres: 'BRAYAN SALVADOR' },
  { apellidos: 'ARBITRO VASQUEZ', nombres: 'GILLARY MISHEL' },
  { apellidos: 'BARRIOS COLOS', nombres: 'EDSALDE ZARELIZ' },
  { apellidos: 'BERROSPI ESCRIBA', nombres: 'SALVADOR ESTEFANO' },
  { apellidos: 'CALDERON URBINA', nombres: 'CAMILA ANTONELLA' },
  { apellidos: 'CASTANEDA CANO', nombres: 'ALESSANDRO' },
  { apellidos: 'ESPINOZA JIMENEZ', nombres: 'FABRIZZIO ALESSANDRO' },
  { apellidos: 'FRIAS ROJAS', nombres: 'JAMILETH NICOL' },
  { apellidos: 'GARCIA URRUTIA', nombres: 'JUAN ALBERTO' },
  { apellidos: 'GUEVARA ALVARADO', nombres: 'ANDREA' },
  { apellidos: 'HERNANDEZ RODRIGUEZ', nombres: 'ISABEL SOFIA' },
  { apellidos: 'HILARIO CHOQUE', nombres: 'THIGO ADRIANO' },
  { apellidos: 'INFANTE RODRIGUEZ', nombres: 'VICTORIA ALEJANDRA' },
  { apellidos: 'LABASTIDAS SANCHEZ', nombres: 'ORIANNY JULIESKA' },
  { apellidos: 'MACO JIMENEZ', nombres: 'YENIFER' },
  { apellidos: 'MENDOZA GALLEGOS', nombres: 'FABRIZIO GERMAN' },
  { apellidos: 'MOGOLLON VALENCIA', nombres: 'MARIANA' },
  { apellidos: 'OLANO PACHERRES', nombres: 'BRAYELY NICOL' },
  { apellidos: 'OYOLA DE LA CRUZ', nombres: 'JOAO DERECK' },
  { apellidos: 'PASACHE ANCAJIMA', nombres: 'FRANCO STEBAN' },
  { apellidos: 'PEREZ PONTE', nombres: 'ALONDRA ARIANA' },
  { apellidos: 'RAMIOS PEREZ', nombres: 'ANAIS ALEXA' },
  { apellidos: 'RAMIREZ ATUNCAR', nombres: 'ABRAHAM ESTEBAN' },
  { apellidos: 'RAMIREZ BANDRES', nombres: 'ANGELES DELEYCAR' },
  { apellidos: 'REYES SOTO', nombres: 'LEONEL JAIRO' },
  { apellidos: 'SABINO CHUQUIZUTA', nombres: 'EVANS ANDRE' },
  { apellidos: 'SALDARRIAGA ESPINOZA', nombres: 'JOSE LUIS' },
  { apellidos: 'SILVA MACUMA', nombres: 'MARLON DAVID' },
  { apellidos: 'SUAREZ LANDA', nombres: 'MILEY AYELEN' },
  { apellidos: 'SUCLUPE VALENCIA', nombres: 'JAN POOL' },
  { apellidos: 'TARAZONA HUERTA', nombres: 'JANETH JASURI' }
];

const alumnos1D = [
  { apellidos: 'AYALA ESPINOZA', nombres: 'DALESSANDRO' },
  { apellidos: 'AYASTA MORE', nombres: 'JOHANA VICTORIA' },
  { apellidos: 'BUSTAMANTE CANDIA', nombres: 'NAHUEL BENJAMIN' },
  { apellidos: 'CAPCHA GUEVARA', nombres: 'EVANNS IKER' },
  { apellidos: 'CONTRERAS CHAVEZ', nombres: 'BRYAN JOSEPH' },
  { apellidos: 'CUYA HUAYNALAYA', nombres: 'ALINA SAHORI' },
  { apellidos: 'ESQUIVEL HINOSTROZA', nombres: 'JAMES ANTONY' },
  { apellidos: 'GONZALES SALAZAR', nombres: 'GENESIS FABIOLA' },
  { apellidos: 'GUERRERO GUTIERREZ', nombres: 'CARELY JESUS' },
  { apellidos: 'HUAMAN ZORILLA', nombres: 'JUAN CRISTIANO' },
  { apellidos: 'HUMALI VARGAS', nombres: 'EMILY' },
  { apellidos: 'JUAREZ GONZALES', nombres: 'FABIAN ALEXANDER' },
  { apellidos: 'LOZA SOLANO', nombres: 'BRYANA CRISTAL' },
  { apellidos: 'MORENO LOPEZ', nombres: 'SNEIDER DAYIRO' },
  { apellidos: 'NAMUCHE DURAND', nombres: 'ASTRID MICHELLE' },
  { apellidos: 'PADILLA NORIEGA', nombres: 'FABIO NICOLA' },
  { apellidos: 'PEREZ LOPEZ', nombres: 'ALEXANDRA FABIOLA' },
  { apellidos: 'PRINCIPE AVILA', nombres: 'MAYRA TALIA' },
  { apellidos: 'QUINTANA BRICENO', nombres: 'ANYURIS ANDRIANNYS' },
  { apellidos: 'QUIROZ CHOTA', nombres: 'SALMA WINIBELL' },
  { apellidos: 'RAMIREZ CONTRERAS', nombres: 'BARBARA MINA' },
  { apellidos: 'REGUERA ESPEJO', nombres: 'DAVID CRISTIAN RAUL' },
  { apellidos: 'RODRIEZ NUNEZ', nombres: 'VICTOR GABRIEL' },
  { apellidos: 'ROJAS LOPEZ', nombres: 'DIEGO EDUARDO' },
  { apellidos: 'SANCHEZ ACOSTA', nombres: 'ERVIN NEYMAR' },
  { apellidos: 'SAUCEDO ZAMORA', nombres: 'GIAN FRANCO' },
  { apellidos: 'SIMON GALVEZ', nombres: 'MELANY' },
  { apellidos: 'TAMANI ARIMUYA', nombres: 'SAYURI ALEJANDRA' },
  { apellidos: 'TICONA DAMIAN', nombres: 'RUTH MAHAL' },
  { apellidos: 'TORRES ARGUEDAS', nombres: 'GABRIEL AARON' },
  { apellidos: 'VASQUEZ SAAVEDRA', nombres: 'FABIANO FRANCISCO' }
];

const alumnos1E = [
  { apellidos: 'AMANCA CARPENA', nombres: 'LEONARDO JOSUE ANGEL' },
  { apellidos: 'BALDERA CESPEDES', nombres: 'KAROLEHY' },
  { apellidos: 'BERRIOS CASTILLO', nombres: 'ALAN DAMIAN' },
  { apellidos: 'CALDERON URBINA', nombres: 'JESUS ENNODIO' },
  { apellidos: 'CARBAJAL PALACIOS', nombres: 'THIAGO FERNANDO' },
  { apellidos: 'DAGA GONZALES', nombres: 'STIFEL HENDRICH' },
  { apellidos: 'DIAZ CACHA', nombres: 'YOLY SAHORI' },
  { apellidos: 'ESPINO SALAZAR', nombres: 'JESUS CLEMENTE' },
  { apellidos: 'FARRO LEON', nombres: 'ALEXIS JUNIOR' },
  { apellidos: 'HERRERA TAFUR', nombres: 'SEBASTIAN VALENTINO' },
  { apellidos: 'HUAMAN PINAN', nombres: 'DARIANA YADIRA' },
  { apellidos: 'JIMENEZ SALDANA', nombres: 'THIAGO PAUL' },
  { apellidos: 'LEON GABRIEL', nombres: 'EDGAR FABRICIO' },
  { apellidos: 'MILLA VILLALVA', nombres: 'RIHANNA THAISA' },
  { apellidos: 'MOLINA SANCHEZ', nombres: 'FRANYELIS ARANZA' },
  { apellidos: 'MURO HUAMAN', nombres: 'MILAN AMIR' },
  { apellidos: 'PARRA CONDORCHOA', nombres: 'MADELEY CIELO' },
  { apellidos: 'PEREZ LUCENA', nombres: 'JOSE MANUEL' },
  { apellidos: 'PUMATAY OLSEN', nombres: 'LUIS ADRIAN' },
  { apellidos: 'QUINONES PENA', nombres: 'SAYAKA TAIMARA' },
  { apellidos: 'QUIROZ MESTANZA', nombres: 'NADINNE YAMILETH' },
  { apellidos: 'REYES MARTINEZ', nombres: 'GERARD FERNANDO' },
  { apellidos: 'ROALCABA MALDONADO', nombres: 'LUIS ANGEL' },
  { apellidos: 'ROJAS BETANCOURT', nombres: 'CRISTOPHER' },
  { apellidos: 'SALINAS CERNA', nombres: 'KATHLEEN DANALEE' },
  { apellidos: 'SAUCEDO VASQUES', nombres: 'YHARIEL JHOMAR' },
  { apellidos: 'SERAFIN MARTINEZ', nombres: 'REYCON DAVID' },
  { apellidos: 'SIESQUEN MONCADA', nombres: 'DARYELY ANDREA' },
  { apellidos: 'TRUJILLO GARCIA', nombres: 'ALEXIS JORDY' },
  { apellidos: 'VERASTEGUI RUELAS', nombres: 'HILLARY SAMARA' },
  { apellidos: 'VILLENA MELGAR', nombres: 'HASAEL ENMANUEL' }
];

const alumnos4A = [
  { apellidos: 'ABANTO BONO', nombres: 'MATHIAS MIGUEL', email: 'abantobono@peruanosuizo.edu.pe' },
  { apellidos: 'AGURTO SALAZAR', nombres: 'XIONARA', email: 'agurtosalazar@peruanosuizo.edu.pe' },
  { apellidos: 'ANAYA ALVARADO', nombres: 'SHEYLA PARIS', email: 'anayaalvarado@peruanosuizo.edu.pe' },
  { apellidos: 'BRAVO MITMA', nombres: 'RENATO', email: 'bravomitma@peruanosuizo.edu.pe' },
  { apellidos: 'BUSTAMANTE CANDIA', nombres: 'WESLEY RYAN THIAGO', email: 'bustamantecandía@peruanosuizo.edu.pe' },
  { apellidos: 'CARO ANGELES', nombres: 'CRISTOPHER DYLAN', email: 'cristopherdylan@peruanosuizo.edu.pe' },
  { apellidos: 'CARRION URBINA', nombres: 'ALIM AKIRA', email: 'alimakira@peruanosuizo.edu.pe' },
  { apellidos: 'CHIPANA ORTIZ', nombres: 'JOHN FRANCISCO', email: 'chipanaortiz@peruanosuizo.edu.pe' },
  { apellidos: 'CHUMPITAZ CORDOVA', nombres: 'JAEL ALEXANDER', email: 'jaelalexander@peruanosuizo.edu.pe' },
  { apellidos: 'CORDOVA MARTINEZ', nombres: 'ARIADNA VALENTINA', email: 'ariadnavalentina@peruanosuizo.edu.pe' },
  { apellidos: 'CRISOSTOMO ESTEBAN', nombres: 'HEYKELL VALENTINO', email: 'crisostomoesteban@peruanosuizo.edu.pe' },
  { apellidos: 'CUBA VASQUEZ', nombres: 'ELIZABETH NALLELY', email: 'elizabethnallely@peruanosuizo.edu.pe' },
  { apellidos: 'CUEVA PEREZ', nombres: 'OZIL RIZZER', email: 'cuevaperez@peruanosuizo.edu.pe' },
  { apellidos: 'DIAZ CRUZADO', nombres: 'LEONARDO DANIEL', email: 'diazcruzado@peruanosuizo.edu.pe' },
  { apellidos: 'ESTELO GAMARRA', nombres: 'KARINA ROMINA', email: 'estelogamarra@peruanosuizo.edu.pe' },
  { apellidos: 'FLORES BENDEZAS', nombres: 'MASSIMO RENATO', email: 'massimorenato@peruanosuizo.edu.pe' },
  { apellidos: 'GAMARRA CUIPAL', nombres: 'CIELO LIZEET', email: 'gamarracuipal@peruanosuizo.edu.pe' },
  { apellidos: 'HILARIO SALINAS', nombres: 'CLAUDIA FERNANDA', email: 'hilariosalinas@peruanosuizo.edu.pe' },
  { apellidos: 'LLAUCE NAZARIO', nombres: 'JAHIR ISMAEL', email: 'llaucenazario@peruanosuizo.edu.pe' },
  { apellidos: 'MORENO RONDON', nombres: 'ARIANNY VALENTINA', email: 'morenorondon@peruanosuizo.edu.pe' },
  { apellidos: 'MUNANTE HUANCA', nombres: 'NIKOLAS', email: 'nikolas@peruanosuizo.edu.pe' },
  { apellidos: 'NIETO SAN MARTIN', nombres: 'VALENTINA JOMAYRA', email: 'nietosanmartin@peruanosuizo.edu.pe' },
  { apellidos: 'OSUNA RODRIGUEZ', nombres: 'JOSE ALFREDO', email: 'osunarodriguez@peruanosuizo.edu.pe' },
  { apellidos: 'PEREZ PEREZ', nombres: 'RENZO ITALO BRUNO', email: 'renzoperez@peruanosuizo.edu.pe' },
  { apellidos: 'RAMIREZ PALOMINO', nombres: 'LUZ YAMILETH', email: 'ramirezpalomino@peruanosuizo.edu.pe' },
  { apellidos: 'RODRIGUEZ AGUILAR', nombres: 'DANIELA', email: 'rodriguezaguilar@peruanosuizo.edu.pe' },
  { apellidos: 'RUIZ HIDALGO', nombres: 'XIOMARA MARIA', email: 'ruizhidalgo@peruanosuizo.edu.pe' },
  { apellidos: 'SICCHA AQUINO', nombres: 'KATHERINE LUANA', email: 'sicchaaquino@peruanosuizo.edu.pe' },
  { apellidos: 'SUAREZ LEVANO', nombres: 'LUANA BELEN', email: 'suarezlevano@peruanosuizo.edu.pe' },
  { apellidos: 'TAPIA MELENDEZ', nombres: 'JULIO GIANFRANCO', email: 'tapiamelendez@peruanosuizo.edu.pe' },
  { apellidos: 'VASQUEZ SOBRINO', nombres: 'GRACE', email: 'vasquezsobrino@peruanosuizo.edu.pe' }
];

const alumnos4B = [
  { apellidos: 'ALCARRAZ APARCO', nombres: 'MARACA FERNANDA', email: 'alcarrazaparco@peruanosuizo.edu.pe' },
  { apellidos: 'CALDERON ALIAGA', nombres: 'OSCAR', email: 'calderonaliaga@peruanosuizo.edu.pe' },
  { apellidos: 'CARHUAPOMA CHAMAYA', nombres: 'NELSI EDITH', email: 'carhuapomachamaya@peruanosuizo.edu.pe' },
  { apellidos: 'CARPIO GARCIA', nombres: 'MIA SUNREY', email: 'carpiogarcia@peruanosuizo.edu.pe' },
  { apellidos: 'CUBAS FLORES', nombres: 'DAYRON JOSE', email: 'cubasflores@peruanosuizo.edu.pe' },
  { apellidos: 'DIONICIO MELGAREJO', nombres: 'DIEGO FORLAN', email: 'diegoforlan@peruanosuizo.edu.pe' },
  { apellidos: 'FLORES BERNUY', nombres: 'AYELEN XIARA', email: 'floresbernuy@peruanosuizo.edu.pe' },
  { apellidos: 'GRIJALVA MAMANI', nombres: 'YESLIN JADE SHANTAL', email: 'grijalmamani@peruanosuizo.edu.pe' },
  { apellidos: 'INSAPILLO PANDURO', nombres: 'JESUS', email: 'insapillopanduro@peruanosuizo.edu.pe' },
  { apellidos: 'JUAREZ QUISPE', nombres: 'JAZMIN YESSENIA', email: 'juarezquispe@peruanosuizo.edu.pe' },
  { apellidos: 'LA CRUZ MELENDEZ', nombres: 'YOARIS NAKARY', email: 'lacruzmelendez@peruanosuizo.edu.pe' },
  { apellidos: 'LOPEZ DURAND', nombres: 'ALBERT GABRIEL', email: 'albertgabriel@peruanosuizo.edu.pe' },
  { apellidos: 'LAM VASQUEZ', nombres: 'JUAN ERNESTO', email: 'juanernesto@peruanosuizo.edu.pe' },
  { apellidos: 'NONTOL CONTRERAS', nombres: 'MARCUS ANDRE', email: 'nontolcontreras@peruanosuizo.edu.pe' },
  { apellidos: 'PFAA FASABI', nombres: 'GENESIS SAMANTHA', email: 'genesissamantha@peruanosuizo.edu.pe' },
  { apellidos: 'PEREZ CABALLERO', nombres: 'THIERI AXEL', email: 'perezcaballero@peruanosuizo.edu.pe' },
  { apellidos: 'PEREZ VARGAS', nombres: 'ASHLEY BRITHANNY', email: 'perezvargas@peruanosuizo.edu.pe' },
  { apellidos: 'PIAAN RAMOS', nombres: 'LUIS ANGEL', email: 'luisangel@peruanosuizo.edu.pe' },
  { apellidos: 'PICOY VIDAL', nombres: 'JUSUE DANIEL', email: 'picoyvidal@peruanosuizo.edu.pe' },
  { apellidos: 'PONCIANO ABAD', nombres: 'LUIS GUSTAVO', email: 'poncianoabad@peruanosuizo.edu.pe' },
  { apellidos: 'QUERO SANCHEZ', nombres: 'EMANTHA VALENTINA', email: 'querosanchez@peruanosuizo.edu.pe' },
  { apellidos: 'QUIROZ BARNICA', nombres: 'MIGUEL OSWALDO', email: 'quirozbarnica@peruanosuizo.edu.pe' },
  { apellidos: 'RENGIFO CAHUA', nombres: 'LUANA ZAHORY', email: 'rengifocahua@peruanosuizo.edu.pe' },
  { apellidos: 'REQUEJO EFFIO', nombres: 'JHOSEP ESMITH', email: 'requejoeffio@peruanosuizo.edu.pe' },
  { apellidos: 'ROJAS MAMANI', nombres: 'MARYORI DAYANNA', email: 'rojasmamani@peruanosuizo.edu.pe' },
  { apellidos: 'SALAS NOVOA', nombres: 'ANITA MARISE', email: 'anitamarise@peruanosuizo.edu.pe' },
  { apellidos: 'SOLSOL BRUAO', nombres: 'MARIELA IVANA', email: 'marielaivana@peruanosuizo.edu.pe' },
  { apellidos: 'SUAREZ PARRA', nombres: 'MARIEL ESTHER', email: 'suarezparra@peruanosuizo.edu.pe' },
  { apellidos: 'TOLEDO BARRENECHEA', nombres: 'JOHAN SANTINO', email: 'toledobarrenechea@peruanosuizo.edu.pe' }
];

const alumnos4C = [
  { apellidos: 'ARNAO HUEYTA', nombres: 'SAIRA GUADALUPE', email: 'arnaohueyta@peruanosuizo.edu.pe' },
  { apellidos: 'BRIONES RODRIGUEZ', nombres: 'KENDRA AYELEN', email: 'brionesrodriguez@peruanosuizo.edu.pe' },
  { apellidos: 'BULNES CAZORLA', nombres: 'ASTRID KHAMYLA', email: 'bulnescazorla@peruanosuizo.edu.pe' },
  { apellidos: 'CABRERA TOLEDO', nombres: 'KATELYN AVRIL', email: 'cabreratoledo@peruanosuizo.edu.pe' },
  { apellidos: 'CANTU QUISPE', nombres: 'EDUARDO ALONSO', email: 'cantuquispe@peruanosuizo.edu.pe' },
  { apellidos: 'CARDENAS PERAMAS', nombres: 'LUHANA GENOVEVA', email: 'cardenasperamas@peruanosuizo.edu.pe' },
  { apellidos: 'CARDENAS YNFANTES', nombres: 'BRENDA HILDA', email: 'cardenasynfantes@peruanosuizo.edu.pe' },
  { apellidos: 'CARHUAZ GUTARRA', nombres: 'JHON SMITH', email: 'carhuazgutarra@peruanosuizo.edu.pe' },
  { apellidos: 'CERCADO GUTIERREZ', nombres: 'FLABIA ARIANA', email: 'cercadogutierrez@peruanosuizo.edu.pe' },
  { apellidos: 'CHAVEZ AGUIRRE', nombres: 'ARNNY JOSEPH', email: 'chavezaguirre@peruanosuizo.edu.pe' },
  { apellidos: 'CUBAS SALAZAR', nombres: 'ARIANNA XIOMARA', email: 'cubassalazar@peruanosuizo.edu.pe' },
  { apellidos: 'ESPINOZA CASTREJON', nombres: 'MARCK VICTOR', email: 'espinozacastrejon@peruanosuizo.edu.pe' },
  { apellidos: 'GARCIA PEREZ', nombres: 'WALTER ADDERLY', email: 'garciaperez@peruanosuizo.edu.pe' },
  { apellidos: 'GOYCOCHEA COSSIO', nombres: 'FERGIE', email: 'goycocheacossio@peruanosuizo.edu.pe' },
  { apellidos: 'GUERRERO MEJIA', nombres: 'ADRIANA ISABEL', email: 'guerreromejia@peruanosuizo.edu.pe' },
  { apellidos: 'IRON VICENTE', nombres: 'STEPHAN JOAQUIN', email: 'ironvicente@peruanosuizo.edu.pe' },
  { apellidos: 'LEON ROJAS MIA', nombres: 'BRIGHIITT DAYANNA', email: 'leonrojasmia@peruanosuizo.edu.pe' },
  { apellidos: 'MARIN LUCIANO', nombres: 'IKER', email: 'marinluciano@peruanosuizo.edu.pe' },
  { apellidos: 'MATOS SEMINO', nombres: 'SARA', email: 'matossemino@peruanosuizo.edu.pe' },
  { apellidos: 'PONTE SINCHI', nombres: 'LUANA SOLEDAD', email: 'pontesinchi@peruanosuizo.edu.pe' },
  { apellidos: 'QUISPE CORNELIO', nombres: 'MARIO FERNANDO', email: 'quispecornelio@peruanosuizo.edu.pe' },
  { apellidos: 'RIOS TITO', nombres: 'GLADIS ROMINA', email: 'riostito@peruanosuizo.edu.pe' },
  { apellidos: 'SALLO DELGADO', nombres: 'JULIET', email: 'sallodelgado@peruanosuizo.edu.pe' },
  { apellidos: 'SANTILLAN ALBUJAR', nombres: 'BELISSA THAIS', email: 'santillanalbujar@peruanosuizo.edu.pe' },
  { apellidos: 'SILVA BETANCOURT', nombres: 'SONISANI', email: 'silvabetancourt@peruanosuizo.edu.pe' },
  { apellidos: 'TOVAR PALOMINO', nombres: 'GENESIS RUTH', email: 'tovarpalomino@peruanosuizo.edu.pe' },
  { apellidos: 'URBINA ARROLLO', nombres: 'ANDRES FERNANDO', email: 'urbinaarrollo@peruanosuizo.edu.pe' },
  { apellidos: 'VARGAS ESPINOZA', nombres: 'WILLIAM ARTURO', email: 'vargasespinoza@peruanosuizo.edu.pe' },
  { apellidos: 'VARGAS TOLEDO', nombres: 'BIANCA ISABEL', email: 'vargastoledo@peruanosuizo.edu.pe' },
  { apellidos: 'VASQUEZ CASTILLO', nombres: 'ASTRID CAMILA', email: 'vasquezcastillo@peruanosuizo.edu.pe' },
  { apellidos: 'VEGA CHERO', nombres: 'ESTIBEN SMITH', email: 'vegachero@peruanosuizo.edu.pe' }
];

const alumnos4D = [
  { apellidos: 'ALARCON FLORES', nombres: 'ANGEL FERNANDO', email: 'alarconflores@peruanosuizo.edu.pe' },
  { apellidos: 'ALVA CORONA', nombres: 'ENRICO DAYAN', email: 'alvacorona@peruanosuizo.edu.pe' },
  { apellidos: 'ANGLAS PEREZ', nombres: 'JEAN PIERRE', email: 'anglasperez@peruanosuizo.edu.pe' },
  { apellidos: 'BALDEON LOPEZ', nombres: 'TOM JEYKO', email: 'baldeonlopez@peruanosuizo.edu.pe' },
  { apellidos: 'BENAVENTE LEVANO', nombres: 'ENRIQUE JOAQUIN', email: 'benaventelevano@peruanosuizo.edu.pe' },
  { apellidos: 'BRAVO QUISPE', nombres: 'CAMILA BELEN', email: 'bravoquispe@peruanosuizo.edu.pe' },
  { apellidos: 'CASAS MONTERO', nombres: 'DAYRON JOSSYMAR', email: 'casasmontero@peruanosuizo.edu.pe' },
  { apellidos: 'DE LA CRUZ GONZALES', nombres: 'ADRIANA PAOLA', email: 'delacruzgonzales@peruanosuizo.edu.pe' },
  { apellidos: 'ESCUDERO HERRERA', nombres: 'ELVIS MIGUEL', email: 'escuderoherrera@peruanosuizo.edu.pe' },
  { apellidos: 'FERREYRA GRANDEZ', nombres: 'MELANNY THAIS', email: 'ferreyragrandez@peruanosuizo.edu.pe' },
  { apellidos: 'GUEVARA GUICOCHEA', nombres: 'LUZ MIRIAN', email: 'guevaraguicochea@peruanosuizo.edu.pe' },
  { apellidos: 'GUZMAN LAZO', nombres: 'ANALUCIA DEL PILAR', email: 'guzmanlazo@peruanosuizo.edu.pe' },
  { apellidos: 'HUAMAN SOTO', nombres: 'FABRICIO', email: 'huamansoto@peruanosuizo.edu.pe' },
  { apellidos: 'HUAPAYA LUDEA', nombres: 'ENZO LEONARDO', email: 'huapayaludena@peruanosuizo.edu.pe' },
  { apellidos: 'JULCARIMA QUISPE', nombres: 'LEONEL', email: 'julcarimaquispe@peruanosuizo.edu.pe' },
  { apellidos: 'MAGUINA CHINO', nombres: 'CYNTHIA HAYMI', email: 'maguinachino@peruanosuizo.edu.pe' },
  { apellidos: 'MEDINA PALOMINO', nombres: 'EIBBY GIOMARA', email: 'medinapalomino@peruanosuizo.edu.pe' },
  { apellidos: 'NOGUERA ARAQUE', nombres: 'WILGREYMARIA VICTORIA', email: 'nogueraaraque@peruanosuizo.edu.pe' },
  { apellidos: 'OLIVERA GUEVARA', nombres: 'DECAR DANIEL', email: 'oliveraguevara@peruanosuizo.edu.pe' },
  { apellidos: 'ORELLANA ESTRADA', nombres: 'ANDRE GILMAR', email: 'orellanaestrada@peruanosuizo.edu.pe' },
  { apellidos: 'PONCIANO JUAREZ', nombres: 'YASUMI ANAHI', email: 'yasumanahi@peruanosuizo.edu.pe' },
  { apellidos: 'QUISPE OSORIO', nombres: 'SHARON LEONELA', email: 'quispeosorio@peruanosuizo.edu.pe' },
  { apellidos: 'QUISPE SULLA', nombres: 'JAZMIN NICOL MARIBEL', email: 'quispesulla@peruanosuizo.edu.pe' },
  { apellidos: 'RIMAS CRUZ', nombres: 'KERLY MICHELLE', email: 'rimascruz@peruanosuizo.edu.pe' },
  { apellidos: 'RODRIGUEZ TELLO', nombres: 'DANIEL ESTEFANO', email: 'rodrigueztello@peruanosuizo.edu.pe' },
  { apellidos: 'ROSALES LOPEZ', nombres: 'BERNABE MAYCOL', email: 'rosaleslopez@peruanosuizo.edu.pe' },
  { apellidos: 'ROSPIGLIOSI CHAVEZ', nombres: 'CARLOS ALBERTO', email: 'rospigliosichavez@peruanosuizo.edu.pe' },
  { apellidos: 'SANTAMARIA ROJAS', nombres: 'JEAN FRANCO', email: 'santamariarojas@peruanosuizo.edu.pe' },
  { apellidos: 'YAYIRE AREVALO', nombres: 'KARMINNA ISABEAU', email: 'yayirearevalo@peruanosuizo.edu.pe' }
];

const alumnos5A = [
  { apellidos: 'AGUILAR ACOSTA', nombres: 'ESTHER VALENTINA', email: 'aguilaracosta@peruanosuizo.edu.pe' },
  { apellidos: 'ALBARRAN DELGADO', nombres: 'KEITHER ARIAN', email: 'albarrandelgado@peruanosuizo.edu.pe' },
  { apellidos: 'ALMENDRAS ALEJANDRO', nombres: 'KEISSY HARUMI', email: 'almendrasalejandro@peruanosuizo.edu.pe' },
  { apellidos: 'ASCUE MORMONTOY', nombres: 'YASURI', email: 'ascuemormontoy@peruanosuizo.edu.pe' },
  { apellidos: 'ASENJO CALDAS', nombres: 'MAYLIN NICOLE', email: 'asenjocaldas@peruanosuizo.edu.pe' },
  { apellidos: 'CARRILLO VILLEGAS', nombres: 'DAYLIN YOLANDA', email: 'carrillovillegas@peruanosuizo.edu.pe' },
  { apellidos: 'CASTILLO MONTALVO', nombres: 'BRYAN ALEXIS', email: 'castillomontalvo@peruanosuizo.edu.pe' },
  { apellidos: 'GONZALES NEIRA', nombres: 'FERNANDA GUISELL', email: 'gonzalesneira@peruanosuizo.edu.pe' },
  { apellidos: 'JURADO PERERA', nombres: 'MARIO ALFONSO', email: 'juradoperera@peruanosuizo.edu.pe' },
  { apellidos: 'LOZANO SANCHEZ', nombres: 'JADE ANGELICA', email: 'lozanosanchez@peruanosuizo.edu.pe' },
  { apellidos: 'MARIN VARA', nombres: 'ALEJANDRO MIGUEL', email: 'marinvara@peruanosuizo.edu.pe' },
  { apellidos: 'MENDOZA FERNANDEZ', nombres: 'LHIBNY GABRIEL', email: 'mendozafernandez@peruanosuizo.edu.pe' },
  { apellidos: 'MILLA ORTIZ', nombres: 'ANDREE JESHUA', email: 'millaortiz@peruanosuizo.edu.pe' },
  { apellidos: 'NOPO VIDAL', nombres: 'YADIRA VICTORIA', email: 'nopovidal@peruanosuizo.edu.pe' },
  { apellidos: 'ORDINOLA ABARCA', nombres: 'IKER ADRIANO', email: 'ordinolaabarca@peruanosuizo.edu.pe' },
  { apellidos: 'PINGUS MENDOZA', nombres: 'JHONATAN', email: 'pingusmendoza@peruanosuizo.edu.pe' },
  { apellidos: 'PIZARRO HUANCA', nombres: 'CARLOS ALBERTO', email: 'pizarrohuanca@peruanosuizo.edu.pe' },
  { apellidos: 'PONTE SICHI', nombres: 'ANTHONY EFRAN', email: 'pontesichi@peruanosuizo.edu.pe' },
  { apellidos: 'REYES TUMPAY', nombres: 'JAIME JOSETH JAIRO', email: 'reyestumpay@peruanosuizo.edu.pe' },
  { apellidos: 'ROJAS SANCHEZ', nombres: 'VALENTINO MANUEL', email: 'rojassanchez@peruanosuizo.edu.pe' },
  { apellidos: 'SAUCEDO CAPCHA', nombres: 'SEBASTIAN ANDRE', email: 'saucedocapcha@peruanosuizo.edu.pe' },
  { apellidos: 'SOLANO CONTRERAS', nombres: 'GIAN CARLOS', email: 'solanocontreras@peruanosuizo.edu.pe' },
  { apellidos: 'TIMOTEO BARRENECHEA', nombres: 'JOSEE LUIZ NOE', email: 'timoteobarrenechea@peruanosuizo.edu.pe' },
  { apellidos: 'TUME ATOCHE', nombres: 'SANDRO ALEXIS', email: 'tumeatoche@peruanosuizo.edu.pe' },
  { apellidos: 'VARGAS ROQUE', nombres: 'JUNMY ALEXANDRA', email: 'vargasroque@peruanosuizo.edu.pe' },
  { apellidos: 'VILLANUEVA MIRAMIRA', nombres: 'GENESIS MILADY', email: 'villanuevamiramira@peruanosuizo.edu.pe' },
  { apellidos: 'YNONAN SEGURA', nombres: 'WILFREDO JOSE', email: 'ynonansegura@peruanosuizo.edu.pe' }
];

const alumnos5B = [
  { apellidos: 'AGUILAR ROJAS', nombres: 'SHANTAL ANAHI', email: 'aguilarrojas@peruanosuizo.edu.pe' },
  { apellidos: 'ALBORNOZVEGA', nombres: 'PRISCILA MAYERLY', email: 'albornozvega@peruanosuizo.edu.pe' },
  { apellidos: 'ALVARON LLANOS', nombres: 'MELANY ARIZETH', email: 'alvaronllanos@peruanosuizo.edu.pe' },
  { apellidos: 'ARCILA CUEVA', nombres: 'SELENE MILUSKA', email: 'arcilacueva@peruanosuizo.edu.pe' },
  { apellidos: 'ARISTA ESCALANTE', nombres: 'GUMTHER RODOLFO', email: 'aristaescalante@peruanosuizo.edu.pe' },
  { apellidos: 'BARRETO SUCLUPE', nombres: 'RODRIGO SEBASTIAN', email: 'barretosuclupe@peruanosuizo.edu.pe' },
  { apellidos: 'BERRU RUIZ', nombres: 'JOSE ANDRES', email: 'berruruiz@peruanosuizo.edu.pe' },
  { apellidos: 'CARRETERO GOMEZ', nombres: 'JAMIN HADDE', email: 'carreterogomez@peruanosuizo.edu.pe' },
  { apellidos: 'COTERA MENDOZA', nombres: 'ANDRES ROLANDO JUNIOR', email: 'coteramendoza@peruanosuizo.edu.pe' },
  { apellidos: 'GREY SANCHEZ', nombres: 'ANAHI MARYORI', email: 'greysanchez@peruanosuizo.edu.pe' },
  { apellidos: 'HORNA MORALES', nombres: 'MARY ANN', email: 'hornamorales@peruanosuizo.edu.pe' },
  { apellidos: 'LUNA MARTINEZ', nombres: 'VICTOR DE JESUS', email: 'lunamartinez@peruanosuizo.edu.pe' },
  { apellidos: 'LUYO VALER', nombres: 'RUBY DULCE MARIA', email: 'luyovaler@peruanosuizo.edu.pe' },
  { apellidos: 'MENDOZA MOCARRO', nombres: 'JEZLI BRIYIT', email: 'mendozamocarro@peruanosuizo.edu.pe' },
  { apellidos: 'MENDOZA OSORIO', nombres: 'MATHIAS AUGUSTO', email: 'mendozaosorio@peruanosuizo.edu.pe' },
  { apellidos: 'PUSSE QUINTO', nombres: 'JOSUE GABRIEL', email: 'pussequinto@peruanosuizo.edu.pe' },
  { apellidos: 'QUINCHO SEGURA', nombres: 'GREYSY MISHEL', email: 'quinchosegura@peruanosuizo.edu.pe' },
  { apellidos: 'ROJAS CARDENAS', nombres: 'TIAGO SEBASTIAN', email: 'rojascardenas@peruanosuizo.edu.pe' },
  { apellidos: 'ROSALES SALDANA', nombres: 'FAVIANO MICHEL', email: 'favianomichel@peruanosuizo.edu.pe' },
  { apellidos: 'ROSALES SALDANA', nombres: 'KENDRA KIRAN', email: 'rosalessaldana@peruanosuizo.edu.pe' },
  { apellidos: 'SANCHEZ HERNANDEZ', nombres: 'JOSUE ALEXIS', email: 'sanchezhernandez@peruanosuizo.edu.pe' },
  { apellidos: 'SIESQUEN ROCHA', nombres: 'JUAN ELEODORO', email: 'siesquenrocha@peruanosuizo.edu.pe' },
  { apellidos: 'SINCHE MAURICIO', nombres: 'ISABEL', email: 'sinchemauricio@peruanosuizo.edu.pe' },
  { apellidos: 'TORRES VALVERDE', nombres: 'MARK DAVID', email: 'torresvalverde@peruanosuizo.edu.pe' },
  { apellidos: 'TRILLO VASQUEZ', nombres: 'RIAN ALEXANDER MARIANO', email: 'trillovasquez@peruanosuizo.edu.pe' },
  { apellidos: 'VALDEZ GAYTAN', nombres: 'SAUL LIONEL', email: 'valdezgaytan@peruanosuizo.edu.pe' },
  { apellidos: 'VALVERDE CORTEZ', nombres: 'JOFRAN XANDER', email: 'valverdecortez@peruanosuizo.edu.pe' },
  { apellidos: 'VILCHEZ GIL', nombres: 'SANDRO ENMANUEL', email: 'vilchezgil@peruanosuizo.edu.pe' },
  { apellidos: 'ZEGARRA LAZO', nombres: 'RUMINA JARITZA', email: 'zegarralazo@peruanosuizo.edu.pe' }
];

const alumnos5C = [
  { apellidos: 'AMBICCHO ASTO', nombres: 'ATHALIA SUSANA VICKY', email: 'ambichoasto@peruanosuizo.edu.pe' },
  { apellidos: 'ANCCO ORTIZ', nombres: 'MARVIN FERNANDO', email: 'anccoortiz@peruanosuizo.edu.pe' },
  { apellidos: 'AZPILCUETA NACCHA', nombres: 'PAVEL', email: 'azpilcuetanaccha@peruanosuizo.edu.pe' },
  { apellidos: 'CABELLO VEAS', nombres: 'CRISTOFER ARIEL', email: 'cabelloveas@peruanosuizo.edu.pe' },
  { apellidos: 'CAMPOS MAMANI', nombres: 'ADRIANO AARON', email: 'camposmamani@peruanosuizo.edu.pe' },
  { apellidos: 'CHURAMPI POLANCO', nombres: 'MATIAS NICOLAS', email: 'churampipolanco@peruanosuizo.edu.pe' },
  { apellidos: 'ENCISO MAMANI', nombres: 'ESTEFANO GABRIEL', email: 'encisomamani@peruanosuizo.edu.pe' },
  { apellidos: 'GARCIA BRUNO', nombres: 'ANDERSON ISRAEL', email: 'garciabruno@peruanosuizo.edu.pe' },
  { apellidos: 'GONZALES CHIRINOS', nombres: 'ANGELINA ANTONELLA', email: 'gonzaleschirinos@peruanosuizo.edu.pe' },
  { apellidos: 'GRACIA NUNEZ', nombres: 'ESTEFANI VALENTINA', email: 'gracianunez@peruanosuizo.edu.pe' },
  { apellidos: 'GRIMALDO VUELTA', nombres: 'LUHANA HARUMI OFELIA', email: 'grimaldovuelta@peruanosuizo.edu.pe' },
  { apellidos: 'GUTIERREZ RIOS', nombres: 'CARLOS JOSUE', email: 'gutierrezrios@peruanosuizo.edu.pe' },
  { apellidos: 'LAROCHE PANTOJA', nombres: 'MICHEL ANTONIO', email: 'larochepantoja@peruanosuizo.edu.pe' },
  { apellidos: 'MEDINA ARBOLEDA', nombres: 'LUHANA GIOMARLY', email: 'medinaarboleda@peruanosuizo.edu.pe' },
  { apellidos: 'MENDOZA CHOQUECONDO', nombres: 'LANETH', email: 'mendozachoquecondo@peruanosuizo.edu.pe' },
  { apellidos: 'MEVONES ATOCHE', nombres: 'MILAGROS DEL ROSARIO', email: 'mevonesatoche@peruanosuizo.edu.pe' },
  { apellidos: 'OLAYA VASQUEZ', nombres: 'JUAN CARLOS SAMUEL', email: 'olayavasquez@peruanosuizo.edu.pe' },
  { apellidos: 'PRADO RODRIGUEZ', nombres: 'ALEXANDER FABIANO', email: 'pradorodriguez@peruanosuizo.edu.pe' },
  { apellidos: 'RABELO ALVAREZ', nombres: 'ALDAYR NICOLAS', email: 'rabeloalvarez@peruanosuizo.edu.pe' },
  { apellidos: 'RAMIREZ DELGADO', nombres: 'RAMON ANTONIO', email: 'ramirezdelgado@peruanosuizo.edu.pe' },
  { apellidos: 'ROMERO VASQUEZ', nombres: 'OSVALDO DAVID', email: 'romerovasquez@peruanosuizo.edu.pe' },
  { apellidos: 'SIMON YAURI', nombres: 'LIONEL MAELO HERNAN', email: 'simonyauri@peruanosuizo.edu.pe' },
  { apellidos: 'TITO GUTIERREZ', nombres: 'DAYANA MABEL', email: 'titogutierrez@peruanosuizo.edu.pe' },
  { apellidos: 'VIGAY NORIEGA', nombres: 'SILVANA', email: 'vigaynoriega@peruanosuizo.edu.pe' },
  { apellidos: 'VILLARREAL CHICLOTE', nombres: 'LEAO ADRIANO', email: 'villarrealchiclote@peruanosuizo.edu.pe' }
];

const alumnos5D = [
  { apellidos: 'ANDRADA YACHACHIN', nombres: 'MARICIELO', email: 'andradayachachin@peruanosuizo.edu.pe' },
  { apellidos: 'ASCENSIO ZACARIAS', nombres: 'PATHIA DANIELA', email: 'ascensiozacarias@peruanosuizo.edu.pe' },
  { apellidos: 'BURGOS BURGOS', nombres: 'JUAN KELVIN', email: 'burgosburgos@peruanosuizo.edu.pe' },
  { apellidos: 'CARBAJAL PONTE', nombres: 'JAIRSINHO JOSUE', email: 'carbajalponte@peruanosuizo.edu.pe' },
  { apellidos: 'CUBAS SALAZAR', nombres: 'ANGEL FABRICIO', email: 'cubassalazar@peruanosuizo.edu.pe' },
  { apellidos: 'DIAZ SAAVEDRA', nombres: 'CAMILA ENITH', email: 'diazsaavedra@peruanosuizo.edu.pe' },
  { apellidos: 'DURAN RODRIGUEZ', nombres: 'ALEXANDER DAVID', email: 'duranrodriguez@peruanosuizo.edu.pe' },
  { apellidos: 'FLORES PUMACAYO', nombres: 'ERYX HEYDAN', email: 'florespumacayo@peruanosuizo.edu.pe' },
  { apellidos: 'GAMBOA ARRIETA', nombres: 'MARIA FERNANDA', email: 'gamboaarrieta@peruanosuizo.edu.pe' },
  { apellidos: 'GONZALES TORRE', nombres: 'MISHELLE DAYANNA', email: 'gonzalestorre@peruanosuizo.edu.pe' },
  { apellidos: 'HURTADO OLIVAS', nombres: 'VALENTINA YAZMILL', email: 'hurtadoolivas@peruanosuizo.edu.pe' },
  { apellidos: 'INSAPILLO PANDURO', nombres: 'JUTSON', email: 'insapillopanduro@peruanosuizo.edu.pe' },
  { apellidos: 'MACO JIMENEZ', nombres: 'AXEL', email: 'axel@peruanosuizo.edu.pe' },
  { apellidos: 'MACO MEZA', nombres: 'FABRIZZIO ALDAIR', email: 'macomeza@peruanosuizo.edu.pe' },
  { apellidos: 'MEZA DELGADO', nombres: 'VANIA', email: 'mezadelgado@peruanosuizo.edu.pe' },
  { apellidos: 'MILIAN CABANILLAS', nombres: 'LEIDY NICOLE', email: 'miliancabanillas@peruanosuizo.edu.pe' },
  { apellidos: 'MORALES PANTA', nombres: 'SARITA TERESA', email: 'moralespanta@peruanosuizo.edu.pe' },
  { apellidos: 'MORENO CABRERA', nombres: 'MISAEL JOSHUE', email: 'misaeljoshue@peruanosuizo.edu.pe' },
  { apellidos: 'OLIVA CASTILLO', nombres: 'ABRAHAM ALVEYRO', email: 'olivacastillo@peruanosuizo.edu.pe' },
  { apellidos: 'PEZO DEL AGUILA DAMARA', nombres: 'DIANA DANIELA', email: 'pezodelaguiladamara@peruanosuizo.edu.pe' },
  { apellidos: 'PISCOYA RODRIGUEZ', nombres: 'DASHELL', email: 'piscoyarodriguez@peruanosuizo.edu.pe' },
  { apellidos: 'QUERO SANCHEZ', nombres: 'ELICEO ANTONIO', email: 'eliceoantonio@peruanosuizo.edu.pe' },
  { apellidos: 'QUISPE TIPTE', nombres: 'DIEGO FERNAND', email: 'quispetipte@peruanosuizo.edu.pe' },
  { apellidos: 'RABELO RIVERA', nombres: 'JEREMY ZANDER', email: 'rabelorivera@peruanosuizo.edu.pe' },
  { apellidos: 'ROJAS VICENTE', nombres: 'JOSE MANUEL', email: 'rojasvicente@peruanosuizo.edu.pe' },
  { apellidos: 'ROMERO RONDONO', nombres: 'NADIA LETICIA', email: 'nadialeticia@peruanosuizo.edu.pe' },
  { apellidos: 'SANCHEZ HUAMANI', nombres: 'SOLANGE', email: 'sanchezhuamani@peruanosuizo.edu.pe' },
  { apellidos: 'SANCHEZ MENDOZA', nombres: 'KARLA ALEJANDRA', email: 'sanchezmendoza@peruanosuizo.edu.pe' },
  { apellidos: 'TREJO CABRERA', nombres: 'DANNA NICOLL', email: 'trejocabrera@peruanosuizo.edu.pe' },
  { apellidos: 'TREJO DIAZ', nombres: 'JHOSEP EMANUEL', email: 'trejodiaz@peruanosuizo.edu.pe' },
  { apellidos: 'VEGA DE LA CRUZ', nombres: 'DAMARIS NICOLLE', email: 'vegadelacruz@peruanosuizo.edu.pe' },
  { apellidos: 'VILCHEZ CORREA', nombres: 'JOAQUIN', email: 'vilchezcorrea@peruanosuizo.edu.pe' }
];

const alumnos5E = [
  { apellidos: 'ABREU AVILA', nombres: 'OSTYN ABRAHAM', email: 'abreuavila@peruanosuizo.edu.pe' },
  { apellidos: 'AREVALO CASTILLO', nombres: 'JOHN PATRICK', email: 'arevalocastillo@peruanosuizo.edu.pe' },
  { apellidos: 'BENDEZU REBATA', nombres: 'KIARA ODALIS', email: 'bendezurebata@peruanosuizo.edu.pe' },
  { apellidos: 'BERNAOLA ROJAS', nombres: 'VICTOR MANUEL', email: 'bernaolarojas@peruanosuizo.edu.pe' },
  { apellidos: 'CACHIQUE VALLEJOS', nombres: 'SAYUMI ESTEFANY', email: 'cachiquevallejos@peruanosuizo.edu.pe' },
  { apellidos: 'CAIRO RAMOS', nombres: 'SEBASTIAN', email: 'cairoramos@peruanosuizo.edu.pe' },
  { apellidos: 'CAMPOS ESCAJADILLO', nombres: 'PEDRO MICHELL', email: 'camposescajadillo@peruanosuizo.edu.pe' },
  { apellidos: 'CARIGA LAZO', nombres: 'CESAR BRAULIO', email: 'carigalazo@peruanosuizo.edu.pe' },
  { apellidos: 'CARRION AQUINO', nombres: 'GERAL JAMES', email: 'carrionaquino@peruanosuizo.edu.pe' },
  { apellidos: 'CUEVA MONTOYA', nombres: 'ISMAEL MAXIMILIANO', email: 'cuevamontoya@peruanosuizo.edu.pe' },
  { apellidos: 'DE LA CRUZ SOLIS', nombres: 'RAINER NICOLAS', email: 'delacruzsolis@peruanosuizo.edu.pe' },
  { apellidos: 'DOROTEO OROYA', nombres: 'YASURI JIMENA', email: 'doroteooroya@peruanosuizo.edu.pe' },
  { apellidos: 'FERNANDEZ YAURI', nombres: 'MICHAELL AARON', email: 'fernandezyauri@peruanosuizo.edu.pe' },
  { apellidos: 'HERNANDEZ RODRIGUEZ', nombres: 'VICTOR JOSE', email: 'hernandezrodriguez@peruanosuizo.edu.pe' },
  { apellidos: 'HILARIO SARMIENTO', nombres: 'NAHOMY FABIOLA', email: 'hilariosarmiento@peruanosuizo.edu.pe' },
  { apellidos: 'HUAMAN MENDOZA', nombres: 'LUANA FERNANDA', email: 'huamanmendoza@peruanosuizo.edu.pe' },
  { apellidos: 'INGA POLO', nombres: 'FABIANA KIMBERLY', email: 'ingapolo@peruanosuizo.edu.pe' },
  { apellidos: 'LINARES VALDIVIA', nombres: 'FABRIZIO FABIAN JAVIER', email: 'linaresvaldivia@peruanosuizo.edu.pe' },
  { apellidos: 'MENESES FERNANDEZ', nombres: 'EDUERLYN DANIELA', email: 'menesesfernandez@peruanosuizo.edu.pe' },
  { apellidos: 'MORENO DIAZ', nombres: 'ANGIE SUHANY', email: 'morenodiaz@peruanosuizo.edu.pe' },
  { apellidos: 'PEREZ RODRIGUEZ', nombres: 'BRYAN RICARDO', email: 'perezrodriguez@peruanosuizo.edu.pe' },
  { apellidos: 'RAFAEL YUPANQUI', nombres: 'ARTHURO FERNANDO MAX', email: 'rafaelyupanqui@peruanosuizo.edu.pe' },
  { apellidos: 'RAZA OTINIANO', nombres: 'YARUMI YAMILE LIZETT', email: 'razaotiniano@peruanosuizo.edu.pe' },
  { apellidos: 'RIVERA SALINAS', nombres: 'ANGELO JOSHUA', email: 'riverasalinas@peruanosuizo.edu.pe' },
  { apellidos: 'ROJAS FERNANDEZ', nombres: 'CHRISTOPHER DE JESUS', email: 'rojasfernandez@peruanosuizo.edu.pe' },
  { apellidos: 'SANCHEZ PENA', nombres: 'TANISHA SOLANGE', email: 'sanchezpena@peruanosuizo.edu.pe' },
  { apellidos: 'SANCHEZ SAAVEDRA', nombres: 'SEBASTIAN MICHEL', email: 'sanchezsaavedra@peruanosuizo.edu.pe' },
  { apellidos: 'SANTISTEBAN CALDERON', nombres: 'RUT NOEMI', email: 'santistebancalderon@peruanosuizo.edu.pe' },
  { apellidos: 'TORRES ZAVALA', nombres: 'BRUNO ALONSO', email: 'torreszavala@peruanosuizo.edu.pe' },
  { apellidos: 'YUCRA CARDENAS', nombres: 'GENESIS VALERIA', email: 'yucracardenas@peruanosuizo.edu.pe' },
  { apellidos: 'ZAVALETA GARRIDO', nombres: 'KEVIN STEVEN', email: 'zavaletagarrido@peruanosuizo.edu.pe' }
];

async function cargarAlumnos() {
  try {
    console.log('🚀 Iniciando carga de alumnos...\n');

    // 1. Obtener año lectivo 2026
    const { data: anio, error: anioError } = await supabase
      .from('anios_lectivos')
      .select('id')
      .eq('nombre', '2026')
      .single();

    if (anioError) {
      console.error('❌ Error al buscar año lectivo:', anioError);
      throw new Error('No se pudo acceder a la tabla anios_lectivos. Verifica los permisos RLS en Supabase.');
    }

    if (!anio) {
      throw new Error('No se encontró el año lectivo 2026');
    }

    console.log('✅ Año lectivo 2026 encontrado:', anio.id);

    // 2. Obtener secciones de 1ro
    const { data: secciones1ro, error: secciones1roError } = await supabase
      .from('secciones')
      .select(`
        id,
        nombre,
        grados!inner (nombre)
      `)
      .eq('grados.nombre', '1ro');

    if (secciones1roError) {
      console.error('❌ Error al buscar secciones de 1ro:', secciones1roError);
      throw new Error('No se pudo acceder a las secciones de 1ro');
    }

    console.log('✅ Secciones de 1ro encontradas:', secciones1ro?.length);

    const seccion1A = secciones1ro?.find(s => s.nombre === 'A');
    const seccion1B = secciones1ro?.find(s => s.nombre === 'B');
    const seccion1C = secciones1ro?.find(s => s.nombre === 'C');
    const seccion1D = secciones1ro?.find(s => s.nombre === 'D');
    const seccion1E = secciones1ro?.find(s => s.nombre === 'E');

    // 3. Obtener secciones de 4to
    const { data: secciones4to, error: secciones4toError } = await supabase
      .from('secciones')
      .select(`
        id,
        nombre,
        grados!inner (nombre)
      `)
      .eq('grados.nombre', '4to');

    if (secciones4toError) {
      console.error('❌ Error al buscar secciones de 4to:', secciones4toError);
      throw new Error('No se pudo acceder a las secciones de 4to');
    }

    console.log('✅ Secciones de 4to encontradas:', secciones4to?.length);

    const seccion4A = secciones4to?.find(s => s.nombre === 'A');
    const seccion4B = secciones4to?.find(s => s.nombre === 'B');
    const seccion4C = secciones4to?.find(s => s.nombre === 'C');
    const seccion4D = secciones4to?.find(s => s.nombre === 'D');

    // 4. Obtener secciones de 5to
    const { data: secciones5to, error: secciones5toError } = await supabase
      .from('secciones')
      .select(`
        id,
        nombre,
        grados!inner (nombre)
      `)
      .eq('grados.nombre', '5to');

    if (secciones5toError) {
      console.error('❌ Error al buscar secciones de 5to:', secciones5toError);
      throw new Error('No se pudo acceder a las secciones de 5to');
    }

    console.log('✅ Secciones de 5to encontradas:', secciones5to?.length);

    const seccion5A = secciones5to?.find(s => s.nombre === 'A');
    const seccion5B = secciones5to?.find(s => s.nombre === 'B');
    const seccion5C = secciones5to?.find(s => s.nombre === 'C');
    const seccion5D = secciones5to?.find(s => s.nombre === 'D');
    const seccion5E = secciones5to?.find(s => s.nombre === 'E');

    if (!seccion1A || !seccion1B || !seccion1C || !seccion1D || !seccion1E) {
      throw new Error('No se encontraron todas las secciones de 1ro (A, B, C, D, E)');
    }

    // COMENTADO: 1ro ya está cargado
    // console.log('📚 Cargando 1ro A...');
    // await cargarSeccion(alumnos1A, seccion1A.id, anio.id, '1A');
    // console.log('✅ 1ro A: 32 alumnos cargados\n');

    // console.log('📚 Cargando 1ro B...');
    // await cargarSeccion(alumnos1B, seccion1B.id, anio.id, '1B');
    // console.log('✅ 1ro B: 32 alumnos cargados\n');

    // console.log('📚 Cargando 1ro C...');
    // await cargarSeccion(alumnos1C, seccion1C.id, anio.id, '1C');
    // console.log('✅ 1ro C: 32 alumnos cargados\n');

    // console.log('📚 Cargando 1ro D...');
    // await cargarSeccion(alumnos1D, seccion1D.id, anio.id, '1D');
    // console.log('✅ 1ro D: 31 alumnos cargados\n');

    // console.log('📚 Cargando 1ro E...');
    // await cargarSeccion(alumnos1E, seccion1E.id, anio.id, '1E');
    // console.log('✅ 1ro E: 31 alumnos cargados\n');

    console.log('⏭️  Saltando 1ro (ya cargado)\n');

    // 10. Cargar 4to A
    if (seccion4A) {
      console.log('📚 Cargando 4to A...');
      await cargarSeccion(alumnos4A, seccion4A.id, anio.id, '4A');
      console.log('✅ 4to A: 31 alumnos cargados\n');
    }

    // 11. Cargar 4to B
    if (seccion4B) {
      console.log('📚 Cargando 4to B...');
      await cargarSeccion(alumnos4B, seccion4B.id, anio.id, '4B');
      console.log('✅ 4to B: 29 alumnos cargados\n');
    }

    // 12. Cargar 4to C
    if (seccion4C) {
      console.log('📚 Cargando 4to C...');
      await cargarSeccion(alumnos4C, seccion4C.id, anio.id, '4C');
      console.log('✅ 4to C: 31 alumnos cargados\n');
    }

    // 13. Cargar 4to D
    if (seccion4D) {
      console.log('📚 Cargando 4to D...');
      await cargarSeccion(alumnos4D, seccion4D.id, anio.id, '4D');
      console.log('✅ 4to D: 29 alumnos cargados\n');
    }

    // 14. Cargar 5to A
    if (seccion5A) {
      console.log('📚 Cargando 5to A...');
      await cargarSeccion(alumnos5A, seccion5A.id, anio.id, '5A');
      console.log('✅ 5to A: 27 alumnos cargados\n');
    }

    // 15. Cargar 5to B
    if (seccion5B) {
      console.log('📚 Cargando 5to B...');
      await cargarSeccion(alumnos5B, seccion5B.id, anio.id, '5B');
      console.log('✅ 5to B: 29 alumnos cargados\n');
    }

    // 16. Cargar 5to C
    if (seccion5C) {
      console.log('📚 Cargando 5to C...');
      await cargarSeccion(alumnos5C, seccion5C.id, anio.id, '5C');
      console.log('✅ 5to C: 25 alumnos cargados\n');
    }

    // 17. Cargar 5to D
    if (seccion5D) {
      console.log('📚 Cargando 5to D...');
      await cargarSeccion(alumnos5D, seccion5D.id, anio.id, '5D');
      console.log('✅ 5to D: 32 alumnos cargados\n');
    }

    // 18. Cargar 5to E
    if (seccion5E) {
      console.log('📚 Cargando 5to E...');
      await cargarSeccion(alumnos5E, seccion5E.id, anio.id, '5E');
      console.log('✅ 5to E: 31 alumnos cargados\n');
    }

    console.log('🎉 ¡Seed completado exitosamente!');
    console.log('📊 Total: 422 alumnos cargados (1ro: 158, 4to: 120, 5to: 144)');
  } catch (error) {
    console.error('❌ Error:', error);
  }
}

async function cargarSeccion(
  alumnos: any[],
  seccionId: string,
  anioId: string,
  codigoPrefix: string
) {
  let exitosos = 0;
  let fallidos = 0;

  // Determinar base de DNI según el grado y sección (secuencial)
  let dniBase = 0;
  if (codigoPrefix === '1A') dniBase = 0;      // 00000001-00000032
  else if (codigoPrefix === '1B') dniBase = 32;   // 00000033-00000064
  else if (codigoPrefix === '1C') dniBase = 64;   // 00000065-00000096
  else if (codigoPrefix === '1D') dniBase = 96;   // 00000097-00000127
  else if (codigoPrefix === '1E') dniBase = 127;  // 00000128-00000158
  else if (codigoPrefix === '4A') dniBase = 158;  // 00000159-00000189
  else if (codigoPrefix === '4B') dniBase = 189;  // 00000190-00000218
  else if (codigoPrefix === '4C') dniBase = 218;  // 00000219-00000249
  else if (codigoPrefix === '4D') dniBase = 249;  // 00000250-00000278
  else if (codigoPrefix === '5A') dniBase = 278;  // 00000279-00000305
  else if (codigoPrefix === '5B') dniBase = 305;  // 00000306-00000334
  else if (codigoPrefix === '5C') dniBase = 334;  // 00000335-00000359
  else if (codigoPrefix === '5D') dniBase = 359;  // 00000360-00000391
  else if (codigoPrefix === '5E') dniBase = 391;  // 00000392-00000422

  for (let i = 0; i < alumnos.length; i++) {
    try {
      const alumno = alumnos[i];
      const numero = String(i + 1).padStart(3, '0');
      const dni = String(dniBase + i + 1).padStart(8, '0');
      let email = alumno.email || `${alumno.apellidos.toLowerCase().replace(/\s+/g, '')}@peruanosuizo.edu.pe`;

      // Intentar crear persona, si el email está duplicado, agregar sufijo
      let persona = null;
      let emailIntento = 1;
      
      while (!persona && emailIntento <= 10) {
        const emailFinal = emailIntento === 1 ? email : email.replace('@', `${emailIntento}@`);
        
        const { data: personaData, error: errorPersona } = await supabase
          .from('personas')
          .insert({
            dni,
            nombres: alumno.nombres,
            apellidos: alumno.apellidos,
            correo: emailFinal,
            fecha_nacimiento: '2010-01-01'
          })
          .select()
          .single();

        if (errorPersona) {
          if (errorPersona.message.includes('personas_correo_key')) {
            // Email duplicado, intentar con sufijo
            emailIntento++;
            continue;
          } else {
            // Otro error
            if (i === 0) console.log(`\n   ⚠️  Error: ${errorPersona.message}`);
            fallidos++;
            break;
          }
        }
        
        persona = personaData;
      }

      if (!persona) continue;

      // 2. Crear alumno
      const { data: alumnoCreado, error: errorAlumno } = await supabase
        .from('alumnos')
        .insert({
          persona_id: persona.id,
          codigo_alumno: `${codigoPrefix}${numero}`,
          fecha_ingreso: '2026-03-01',
          estado: 'activo'
        })
        .select()
        .single();

      if (errorAlumno) {
        if (i === 0) console.log(`\n   ⚠️  Error: ${errorAlumno.message}`);
        fallidos++;
        continue;
      }

      // 3. Crear matrícula
      const { error: errorMatricula } = await supabase
        .from('matriculas')
        .insert({
          alumno_id: alumnoCreado.id,
          seccion_id: seccionId,
          anio_lectivo_id: anioId,
          fecha_matricula: '2026-03-01',
          estado: 'activo'
        });

      if (errorMatricula) {
        if (i === 0) console.log(`\n   ⚠️  Error: ${errorMatricula.message}`);
        fallidos++;
        continue;
      }

      // 4. Crear código QR
      await supabase
        .from('codigos_qr')
        .insert({
          persona_id: persona.id,
          codigo: `QR${codigoPrefix}${numero}`,
          activo: true
        });

      exitosos++;
      process.stdout.write(`\r   Progreso: ${exitosos}/${alumnos.length} (${fallidos} omitidos)`);
    } catch (error) {
      fallidos++;
    }
  }
  console.log(''); // Nueva línea
}

// Ejecutar
cargarAlumnos();
