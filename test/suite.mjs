// MobSquad acceptance suite. Usage: node suite.mjs <26.1.2|26.2|26.3> [only=section,section]
// Builds a throwaway server on a flat world from downloaded jars and drives it with bots.
//
// What's real and what isn't:
// - Bots speak 1.21.11 through ViaVersion and ViaBackwards. They click entities, blocks and menu slots like
//   players, but they don't draw anything: how the menu and the critters look is untested here.
// - On 26.3 the bots can't send movement (see harness.mjs), so they hover where /tp puts them. Walking a
//   chicken is done with small /tp steps on every version, and the sprint check runs on 26.1.2 and 26.2 only.
// - On 26.3 the bots can't use a block either (PaulBunyan's suite found this first), so the Bunkhouse menu and
//   everything driven through it (sending out, following, safety, restarts) runs on 26.1.2 and 26.2 only.
import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { Vec3 } from 'vec3'
import { bot, check, DEFAULT_CONFIG, freshServer, jar, quit, section, Server, sleep, summary, until } from './harness.mjs'

const MC = process.argv[2]
if (!['26.1.2', '26.2', '26.3'].includes(MC)) throw new Error('usage: node suite.mjs <26.1.2|26.2|26.3> [only=a,b]')
const ONLY = (process.argv.find(a => a.startsWith('only=')) || '').slice(5).split(',').filter(Boolean)
const want = n => ONLY.length === 0 || ONLY.includes(n)
const PHYSICS = MC !== '26.3'
const MENU = PHYSICS
const skip = name => { if (want(name)) console.log(`\n# ${name}: skipped on ${MC}, where the bots can't use a block`) }
const Y = -60

const dir = freshServer('suite-' + MC, { paper: jar(`paper-${MC}.jar`),
  plugins: [jar('ViaVersion.jar'), jar('ViaBackwards.jar'), jar('GriefPrevention.jar')], props: { difficulty: 'easy' } })
const s = new Server({ dir, jvm: ['-Dmobsquad.test.sneak-tag=true'] }) // a tagged player counts as crouching; see Puzzles.crouching
const cfgFile = path.join(dir, 'plugins/MobSquad/config.yml')
const playersDir = path.join(dir, 'plugins/MobSquad/players')
const baseConfig = readFileSync(DEFAULT_CONFIG, 'utf8')
async function config(changes = {}) {
  let text = baseConfig
  for (const [k, v] of Object.entries(changes)) {
    const re = new RegExp(`^${k}: .*$`, 'm')
    if (!re.test(text)) throw new Error('no setting ' + k)
    text = text.replace(re, `${k}: ${v}`)
  }
  writeFileSync(cfgFile, text)
  const out = (await s.ask('mobsquad reload', 800)).join(' ')
  if (!/reloaded/.test(out)) throw new Error('reload failed: ' + out)
}
/** A player's MobSquad file, read straight off the disk. */
function file(name) {
  if (!existsSync(playersDir)) return ''
  for (const f of readdirSync(playersDir, { withFileTypes: true }).filter(e => e.isFile() && e.name.endsWith('.txt'))) {
    const text = readFileSync(path.join(playersDir, f.name), 'utf8')
    if (text.includes('\n') && f.name.startsWith(uuids[name])) return text
  }
  return ''
}
const critterLines = name => file(name).split('\n').filter(l => l.startsWith('critter '))
const uuids = {}, intIds = {}
async function uuidOf(name) {
  const out = (await s.ask(`data get entity ${name} UUID`, 400)).join(' ')
  const m = out.match(/\[I; (-?\d+), (-?\d+), (-?\d+), (-?\d+)\]/)
  if (!m) throw new Error('no UUID for ' + name + ': ' + out)
  const hex = m.slice(1).map(n => (Number(n) >>> 0).toString(16).padStart(8, '0')).join('')
  intIds[name] = m.slice(1).join(', ')
  uuids[name] = `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}
/** Right-clicks an entity the way a real client does, with the spot on it that was clicked. */
const poke = (b, e) => b.activateEntityAt(e, e.position.offset(0, 0.3, 0))
const said = (b, re, from = 0) => b.chatLog.slice(from).some(m => re.test(m))
const waitSaid = (b, re, ms = 8000, from = 0) => until(() => said(b, re, from), ms)
async function health(selector) {
  const out = (await s.ask(`data get entity ${selector} Health`, 400)).join(' ')
  const m = out.match(/data: ([\d.]+)f/)
  return m ? +m[1] : null
}

console.log(`MobSquad suite on Paper ${MC}, bots ${PHYSICS ? 'with' : 'without'} physics, in ${dir}`)
await s.start()
check('MobSquad enabled', s.log.some(l => /Enabling MobSquad/.test(l)))
for (const g of ['doDaylightCycle false', 'advance_time false', 'doMobSpawning false', 'spawn_mobs false', 'doWeatherCycle false'])
  await s.ask('gamerule ' + g, 150)
await s.ask('time set noon', 200)
await config({ 'recovery-minutes': 5 })

let me = await bot('Wrangler', { physics: PHYSICS })
let other = await bot('Neighbor', { physics: PHYSICS })
if (process.env.MS_TEST_HURT) me.on('health', () => { if (me.health < 20) console.log(`    [hurt] health ${me.health}; server: ${s.log.slice(-3).join(' / ').slice(0, 300)}`) })
await uuidOf('Wrangler'); await uuidOf('Neighbor')
await s.ask('gamemode survival Wrangler'); await s.ask('gamemode survival Neighbor')

const X = 3000, Z = 3000
await s.ask(`forceload add ${X - 64} ${Z - 64} ${X + 64} ${Z + 64}`, 1500)
await until(async () => (await s.ask(`execute if loaded ${X} ${Y} ${Z}`, 200)).join(' ').includes('passed'), 20000, 300)
async function stand(b, name, x, z, yaw = -90, pitch = 10) {
  await s.ask(`tp ${name} ${x + 0.5} ${Y} ${z + 0.5} ${yaw} ${pitch}`, 300)
  await until(() => b.entity.position.distanceTo(new Vec3(x + 0.5, Y, z + 0.5)) < 1.5, 8000)
}
/** Walks a player along +x with small steps, at walking pace, so it's never sprinting. */
async function walk(name, x, z, blocks) {
  for (let i = 1; i <= blocks * 2; i++) { await s.ask(`tp ${name} ${x + 0.5 + i / 2} ${Y} ${z + 0.5} -90 10`, 0); await sleep(220) }
}
async function hold(b, name, item, count = 16) {
  await s.ask(`clear ${name}`, 200)
  await s.ask(`give ${name} minecraft:${item} ${count}`, 300)
  await until(() => b.inventory.items().some(i => i.name === item), 5000)
  await b.equip(b.inventory.items().find(i => i.name === item), 'hand')
}
function entity(b, pred) { return Object.values(b.entities).find(pred) }
async function openBunkhouse(b, at) {
  const block = b.blockAt(at)
  const opened = new Promise((ok, fail) => { b.once('windowOpen', ok); setTimeout(() => fail(new Error('no menu')), 6000) })
  await b.activateBlock(block)
  return opened
}
const egg = (w, slot) => w.slots[slot]?.name
async function closeMenu(b) { if (b.currentWindow) b.closeWindow(b.currentWindow); await sleep(300) }

// ---------- Bunkhouse ----------
const vault = new Vec3(X, Y, Z + 3)
if (want('bunkhouse')) {
  section('Bunkhouses')
  await s.ask(`setblock ${vault.x} ${vault.y} ${vault.z} minecraft:vault`, 300)
  await s.ask('op Wrangler', 300)
  await stand(me, 'Wrangler', X, Z, 0, 20) // facing south, at the vault
  let from = me.chatLog.length
  me.chat('/mobsquad bunkhouse add')
  check('an admin marks a vault as a Bunkhouse', await waitSaid(me, /Bunkhouse now/, 5000, from), me.chatLog.slice(from).join(' | '))
  from = me.chatLog.length
  me.chat('/mobsquad bunkhouse add')
  check('marking it twice says so', await waitSaid(me, /already a Bunkhouse/, 5000, from))
  await s.ask('deop Wrangler', 300)
  from = other.chatLog.length
  other.chat('/mobsquad bunkhouse add')
  await sleep(1200)
  check('a player without mobsquad.admin can\'t mark one', !said(other, /Bunkhouse now/, from), other.chatLog.slice(from).join(' | '))
  if (MENU) {
  const w = await openBunkhouse(me, vault)
  check('right-clicking a Bunkhouse opens the menu', /Bunkhouse/.test(JSON.stringify(w.title)), JSON.stringify(w.title))
  check('a new player\'s Bunkhouse is empty', egg(w, 0) === undefined, egg(w, 0))
  check('the menu has the info book and the call-back bell', egg(w, 49) === 'book' && egg(w, 53) === 'bell', `${egg(w, 49)} ${egg(w, 53)}`)
  await closeMenu(me)
  check('a Bunkhouse hands out no vault loot', me.inventory.items().length === 0, JSON.stringify(me.inventory.items().map(i => i.name)))
  }
}

// ---------- coaxing a chicken ----------
if (want('coax')) {
  section('Coaxing a chicken')
  const cx = X + 10, cz = Z + 20
  let lastChicken = -1
  /** A fresh chicken in front of the bot. Waits for the bot to see the new one, not the one just killed. */
  async function chicken(extra = '') {
    await s.ask(`kill @e[type=chicken]`, 300)
    await until(() => !entity(me, e => e.name === 'chicken'), 3000)
    await s.ask(`summon chicken ${cx + 1.5} ${Y} ${cz + 0.5} {NoAI:0b${extra}}`, 600)
    await until(() => entity(me, e => e.name === 'chicken' && e.id > lastChicken), 5000)
    const c = entity(me, e => e.name === 'chicken' && e.id > lastChicken)
    lastChicken = c.id
    return c
  }
  await stand(me, 'Wrangler', cx, cz)
  await hold(me, 'Wrangler', 'wheat_seeds')
  let c = await chicken()
  let from = me.chatLog.length
  await poke(me, c)
  check('seeds and a right-click start a trail', await waitSaid(me, /interested/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('starting a trail uses one seed', me.inventory.items().find(i => i.name === 'wheat_seeds')?.count === 15)
  await walk('Wrangler', cx, cz, 14)
  check('walking it ten blocks coaxes it', await waitSaid(me, /You coaxed a chicken/, 6000, from), me.chatLog.slice(from).join(' | '))
  check('the coaxed chicken leaves the world', await s.count('@e[type=chicken]') === 0)
  check('it\'s saved in the player\'s own file', critterLines('Wrangler').length === 1 && /chicken/.test(critterLines('Wrangler')[0]), file('Wrangler'))
  check('coaxing earns Wranglin\' experience', /wranglin 100/.test(file('Wrangler')), file('Wrangler'))

  // a chicken with a name belongs to someone
  await stand(me, 'Wrangler', cx, cz)
  c = await chicken(',CustomName:"Henrietta"')
  from = me.chatLog.length
  await poke(me, c)
  check('a named chicken can\'t be coaxed', await waitSaid(me, /has a name/, 4000, from), me.chatLog.slice(from).join(' | '))

  // putting the seeds away ends it
  c = await chicken()
  from = me.chatLog.length
  await poke(me, c)
  await waitSaid(me, /interested/, 4000, from)
  me.setQuickBarSlot((me.quickBarSlot + 1) % 9)
  check('putting the seeds away ends the trail', await waitSaid(me, /put the seeds away/, 4000, from), me.chatLog.slice(from).join(' | '))
  await hold(me, 'Wrangler', 'wheat_seeds')

  // wandering too far from it ends it
  await stand(me, 'Wrangler', cx, cz)
  c = await chicken()
  from = me.chatLog.length
  await poke(me, c)
  await waitSaid(me, /interested/, 4000, from)
  await s.ask(`tp Wrangler ${cx + 30.5} ${Y} ${cz + 0.5}`, 300)
  check('leaving the chicken behind ends the trail', await waitSaid(me, /wandered off/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('a failed trail coaxes nothing', critterLines('Wrangler').length === 1)

  if (PHYSICS) {
    await stand(me, 'Wrangler', cx, cz)
    c = await chicken()
    from = me.chatLog.length
    await poke(me, c)
    await waitSaid(me, /interested/, 4000, from)
    me.setControlState('sprint', true); me.setControlState('forward', true)
    const ran = await waitSaid(me, /Too fast/, 4000, from)
    me.clearControlStates()
    check('running ends the trail', ran, me.chatLog.slice(from).join(' | '))
  }

  // on someone else's claim, the claim plugin says no first
  await s.ask('kill @e[type=chicken]', 300)
  await stand(other, 'Neighbor', X + 40, Z + 40)
  await hold(other, 'Neighbor', 'golden_shovel', 1)
  let nfrom = other.chatLog.length
  await s.ask('acb Neighbor 400', 300) // enough claim blocks for an 11 by 11 claim
  other.chat('/claim 5') // GriefPrevention's smallest claim around the player
  const made = await waitSaid(other, /[Cc]laim created|created/, 5000, nfrom)
  await stand(me, 'Wrangler', X + 38, Z + 40)
  await s.ask(`summon chicken ${X + 39.5} ${Y} ${Z + 40.5}`, 600)
  await until(() => entity(me, e => e.name === 'chicken'), 5000)
  from = me.chatLog.length
  await poke(me, entity(me, e => e.name === 'chicken'))
  await sleep(1500)
  if (made) check('a chicken on someone else\'s claim can\'t be coaxed', !said(me, /interested/, from), me.chatLog.slice(from).join(' | '))
  else console.log('  (skipped the claim check: GriefPrevention made no claim: ' + other.chatLog.slice(nfrom).join(' | ') + ')')
  await s.ask('kill @e[type=chicken]', 300)
}

// ---------- sending out and following ----------
if (!MENU) skip('posse')
else if (want('posse')) {
  section('Sending critters out')
  if (critterLines('Wrangler').length === 0) await s.ask('mobsquad give Wrangler chicken', 400)
  await s.ask(`clear Wrangler`, 200)
  await stand(me, 'Wrangler', X, Z, 0, 20)
  let w = await openBunkhouse(me, vault)
  check('the coaxed chicken is in the Bunkhouse', egg(w, 0) === 'chicken_spawn_egg', egg(w, 0))
  let from = me.chatLog.length
  await me.clickWindow(0, 0, 0)
  check('clicking it sends it out', await waitSaid(me, /out with you/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('it stands beside its owner', await s.ask('execute as Wrangler at @s if entity @e[type=chicken,name=Chicken,distance=..4]', 400).then(o => o.join(' ').includes('passed')))
  await sleep(500)
  check('the menu icon can\'t be taken', !me.inventory.items().some(i => /spawn_egg/.test(i.name)), JSON.stringify(me.inventory.items().map(i => i.name)))
  await closeMenu(me)

  await s.ask('clear Neighbor', 200)
  await stand(other, 'Neighbor', X + 1, Z, 0, 20)
  const o = await openBunkhouse(other, vault)
  check('another player sees only their own Bunkhouse', egg(o, 0) === undefined, egg(o, 0))
  await closeMenu(other)

  await s.ask('mobsquad give Wrangler chicken', 400)
  w = await openBunkhouse(me, vault)
  from = me.chatLog.length
  await me.clickWindow(1, 0, 0)
  check('at Wranglin\' level 1 only one critter goes out', await waitSaid(me, /1 critter out at once/, 4000, from), me.chatLog.slice(from).join(' | '))
  await closeMenu(me)
  await s.ask('mobsquad xp Wrangler 2250', 400)
  w = await openBunkhouse(me, vault)
  from = me.chatLog.length
  await me.clickWindow(1, 0, 0)
  check('at level 10 a second one goes out', await waitSaid(me, /out with you/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('two chickens are out', await s.count('@e[type=chicken,name=Chicken]') === 2)
  await closeMenu(me)

  await s.ask('mobsquad give Wrangler creeper', 400)
  w = await openBunkhouse(me, vault)
  from = me.chatLog.length
  await me.clickWindow(2, 0, 0)
  check('a Yonder critter waits for Wranglin\' level 25', await waitSaid(me, /level 25/, 4000, from), me.chatLog.slice(from).join(' | '))
  await closeMenu(me)

  section('Following')
  await stand(me, 'Wrangler', X + 12, Z)
  check('critters walk after their owner', await until(async () =>
    (await s.ask('execute as Wrangler at @s if entity @e[type=chicken,name=Chicken,distance=..5]', 300)).join(' ').includes('passed'), 12000, 500))
  await stand(me, 'Wrangler', X + 50, Z)
  check('a critter left far behind catches up', await until(async () =>
    (await s.ask('execute as Wrangler at @s if entity @e[type=chicken,name=Chicken,distance=..6]', 300)).join(' ').includes('passed'), 8000, 500))

  section('Calling back')
  await stand(me, 'Wrangler', X, Z, 0, 20)
  w = await openBunkhouse(me, vault)
  from = me.chatLog.length
  await me.clickWindow(53, 0, 0)
  check('the bell in the menu calls everyone back', await waitSaid(me, /Everyone's back/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('no critter is left in the world', await s.count('@e[type=chicken,name=Chicken]') === 0)
  await closeMenu(me)

  w = await openBunkhouse(me, vault); await me.clickWindow(0, 0, 0); await closeMenu(me)
  from = me.chatLog.length
  me.chat('/mobsquad dinnerbell')
  await until(() => me.inventory.items().some(i => i.name === 'warped_fungus_on_a_stick'), 4000)
  await me.equip(me.inventory.items().find(i => i.name === 'warped_fungus_on_a_stick'), 'hand')
  me.activateItem()
  check('the dinnerbell calls critters home', await waitSaid(me, /headed home/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('nothing is left out after the dinnerbell', await s.count('@e[type=chicken,name=Chicken]') === 0)
  from = me.chatLog.length
  me.chat('/mobsquad list')
  check('/mobsquad list shows the critters', await waitSaid(me, /3 critters/, 4000, from), me.chatLog.slice(from).join(' | '))
}

// ---------- safety ----------
if (!MENU) skip('safety')
else if (want('safety')) {
  section('Safety')
  await s.ask('mobsquad xp Wrangler 15000', 400) // level 25: three out, Yonder unlocked
  for (const k of ['creeper', 'skeleton']) if (!critterLines('Wrangler').some(l => l.includes(' ' + k + ' '))) await s.ask(`mobsquad give Wrangler ${k}`, 400)
  await s.ask('clear Wrangler', 200)
  await stand(me, 'Wrangler', X, Z, 0, 20)
  const lines = critterLines('Wrangler')
  const slotOf = k => lines.findIndex(l => l.includes(' ' + k + ' '))
  let w = await openBunkhouse(me, vault)
  await me.clickWindow(slotOf('creeper'), 0, 0); await sleep(300)
  await me.clickWindow(slotOf('skeleton'), 0, 0); await sleep(300)
  await closeMenu(me)
  check('a creeper and a skeleton are out', await s.count('@e[type=creeper,name=Creeper]') === 1 && await s.count('@e[type=skeleton,name=Skeleton]') === 1)
  me.hurt = 0
  await sleep(10000)
  check('a creeper critter never explodes near its owner', await s.count('@e[type=creeper,name=Creeper]') === 1)
  check('critters never hurt their owner', me.hurt === 0 && me.health === 20, `health ${me.health}`)
  check('a skeleton critter doesn\'t burn in the sun', await s.count('@e[type=skeleton,name=Skeleton]') === 1)
  check('no blocks changed around them', (await s.ask(`execute if block ${X} ${Y - 1} ${Z} minecraft:grass_block`, 300)).join(' ').includes('passed'))

  // the other player walks up: no critter goes for them either
  await stand(other, 'Neighbor', X + 2, Z)
  other.hurt = 0
  await sleep(6000)
  check('critters never hurt other players', other.hurt === 0, `health ${other.health}`)

  // a player can't hurt a critter
  const sk = entity(me, e => e.name === 'skeleton')
  const before = await health('@e[type=skeleton,name=Skeleton,limit=1]')
  if (sk) { await me.attack(sk); await sleep(400); await me.attack(sk); await sleep(600) }
  const after = await health('@e[type=skeleton,name=Skeleton,limit=1]')
  check('players can\'t hurt critters', sk && before !== null && after === before, `${before} -> ${after}`)

  // out of health: tuckered out, not dead
  let from = me.chatLog.length
  await s.ask('kill @e[type=item]', 200)
  await s.ask('damage @e[type=skeleton,name=Skeleton,limit=1] 100 minecraft:generic', 600)
  check('a critter out of health is tuckered out', await waitSaid(me, /tuckered out/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('it goes back to the Bunkhouse', await s.count('@e[type=skeleton]') === 0)
  check('it drops nothing', await s.count('@e[type=item]') === 0 && await s.count('@e[type=experience_orb]') === 0)
  const sline = critterLines('Wrangler').find(l => l.includes(' skeleton '))
  check('its rest is saved', sline && +sline.split(' ')[5] > Date.now(), sline)
  w = await openBunkhouse(me, vault)
  from = me.chatLog.length
  await me.clickWindow(slotOf('skeleton'), 0, 0)
  check('a resting critter can\'t go out', await waitSaid(me, /still resting/, 4000, from), me.chatLog.slice(from).join(' | '))
  await closeMenu(me)
  await config({ 'recovery-minutes': 0 })
}

// ---------- the other launch puzzles ----------
if (want('puzzles')) {
  section('Coaxing the other launch critters')
  await s.ask('gamemode survival Wrangler', 200)
  const px = X - 40, pz = Z - 40
  const count = k => critterLines('Wrangler').filter(l => l.includes(' ' + k + ' ')).length
  async function fresh(type, nbt = '', at = [px + 2.5, Y, pz + 0.5]) {
    await s.ask(`kill @e[type=${type}]`, 300)
    await until(() => !entity(me, e => e.name === type), 3000)
    const before = Math.max(-1, ...Object.values(me.entities).filter(e => e.name === type).map(e => e.id))
    await s.ask(`summon ${type} ${at.join(' ')} ${nbt}`, 600)
    await until(() => entity(me, e => e.name === type && e.id > before), 5000)
    return entity(me, e => e.name === type && e.id > before)
  }
  async function attempt(type, item, nbt, { face = [-90, 10], at, standAt = [px, pz] } = {}) {
    await stand(me, 'Wrangler', standAt[0], standAt[1], face[0], face[1])
    await hold(me, 'Wrangler', item)
    const mob = await fresh(type, nbt, at)
    const from = me.chatLog.length
    await poke(me, mob)
    return { mob, from }
  }

  await s.ask('mobsquad xp Wrangler 0', 300)
  let { from } = await attempt('creeper', 'gunpowder', '{NoAI:0b}')
  check('a Yonder mob needs Wranglin\' level 25 to coax', await waitSaid(me, /from Wranglin' level 25/, 4000, from), me.chatLog.slice(from).join(' | '))
  await s.ask('mobsquad xp Wrangler 40000', 300) // level 40: every tier

  // the creeper standoff
  await s.ask('tag Wrangler add mobsquad-test-sneak', 200)
  await sleep(300)
  ;({ from } = await attempt('creeper', 'gunpowder', ''))
  check('gunpowder starts the creeper standoff', await waitSaid(me, /Crouch while it swells/, 4000, from), me.chatLog.slice(from).join(' | '))
  await s.ask('effect give Wrangler minecraft:instant_health 1 5', 600)
  me.hurt = 0
  const cr = () => entity(me, e => e.name === 'creeper')
  for (let i = 0; i < 3; i++) { await sleep(2600); if (cr()) await poke(me, cr()) }
  check('crouching through three swells and feeding it coaxes the creeper', await waitSaid(me, /You coaxed a creeper/, 4000, from), me.chatLog.slice(from).join(' | '))
  const ground = (await s.ask(`execute if block ${px} ${Y - 1} ${pz} minecraft:grass_block`, 300)).join(' ')
  check('the standoff never blows up', me.hurt === 0 && ground.includes('passed'), `hurt ${me.hurt}, ground ${ground.slice(0, 60)}`)
  ;({ from } = await attempt('creeper', 'gunpowder', ''))
  await waitSaid(me, /Crouch while it swells/, 4000, from)
  await s.ask('tag Wrangler remove mobsquad-test-sneak', 200)
  check('standing up mid-swell starts it over', await waitSaid(me, /stood up mid-swell/, 4000, from), me.chatLog.slice(from).join(' | '))
  await s.ask('tag Wrangler add mobsquad-test-sneak', 200)
  ;({ from } = await attempt('creeper', 'gunpowder', ''))
  await waitSaid(me, /Crouch while it swells/, 4000, from)
  await sleep(500); await poke(me, cr())
  check('gunpowder mid-swell is too soon', await waitSaid(me, /Too soon/, 4000, from), me.chatLog.slice(from).join(' | '))
  ;({ from } = await attempt('creeper', 'gunpowder', ''))
  await waitSaid(me, /Crouch while it swells/, 4000, from)
  await sleep(7500)
  check('forgetting the gunpowder starts it over', said(me, /didn't give it gunpowder/, from), me.chatLog.slice(from).join(' | '))
  check('a creeper left after a failed standoff hasn\'t gone off', me.hurt === 0, `hurt ${me.hurt}`)
  await s.ask('tag Wrangler remove mobsquad-test-sneak', 200)
  await s.ask('kill @e[type=creeper]', 300)

  // the enderman: facing away from it the whole time
  ;({ from } = await attempt('enderman', 'grass_block', '{NoAI:0b}', { face: [90, 0] }))
  check('a block it could carry starts the enderman\'s puzzle', await waitSaid(me, /don't look it in the eye/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('not looking at it for 20 seconds coaxes the enderman', await waitSaid(me, /You coaxed an enderman/, 25000, from), me.chatLog.slice(from).join(' | '))
  ;({ from } = await attempt('enderman', 'grass_block', '{NoAI:1b}', { face: [90, 0] }))
  await waitSaid(me, /don't look it in the eye/, 4000, from)
  await sleep(1500)
  await s.ask(`tp Wrangler ${px + 0.5} ${Y} ${pz + 0.5} -90 -20`, 300) // facing east and up, at its face
  check('looking it in the eye starts it over', await waitSaid(me, /looked it in the eye/, 5000, from), me.chatLog.slice(from).join(' | '))
  await s.ask('kill @e[type=enderman]', 300)

  // the skeleton: arrows from the player into a target block
  await s.ask(`setblock ${px + 6} ${Y} ${pz + 4} minecraft:target`, 300)
  ;({ from } = await attempt('skeleton', 'arrow', '{NoAI:1b}'))
  check('an arrow starts the skeleton\'s puzzle', await waitSaid(me, /fair shot/, 4000, from), me.chatLog.slice(from).join(' | '))
  for (let i = 0; i < 3; i++) {
    await s.ask(`summon arrow ${px + 6.5} ${Y + 3} ${pz + 4.5} {Motion:[0.0,-1.5,0.0],Owner:[I; ${intIds.Wrangler}]}`, 900)
  }
  check('three hits on a target block coax the skeleton', await waitSaid(me, /You coaxed a skeleton/, 5000, from), me.chatLog.slice(from).join(' | '))

  // the iron golem: only one a player built
  ;({ from } = await attempt('iron_golem', 'iron_ingot', '{PlayerCreated:0b,NoAI:1b}'))
  check('a village\'s golem can\'t be coaxed', await waitSaid(me, /belongs to the village/, 4000, from), me.chatLog.slice(from).join(' | '))
  ;({ from } = await attempt('iron_golem', 'iron_ingot', '{PlayerCreated:1b,NoAI:1b}'))
  const golem = entity(me, e => e.name === 'iron_golem')
  for (let i = 0; i < 3; i++) { await sleep(700); await poke(me, golem) }
  await sleep(500)
  await hold(me, 'Wrangler', 'poppy', 1)
  await poke(me, golem)
  check('four iron ingots and a poppy coax a golem a player built', await waitSaid(me, /You coaxed an iron golem/, 4000, from), me.chatLog.slice(from).join(' | '))

  // the fox: crouched the whole time
  await s.ask('tag Wrangler add mobsquad-test-sneak', 200); await sleep(300)
  ;({ from } = await attempt('fox', 'sweet_berries', '{NoAI:1b}'))
  check('crouching with berries starts the fox\'s puzzle', await waitSaid(me, /Stay crouched/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('staying crouched for 15 seconds coaxes the fox', await waitSaid(me, /You coaxed a fox/, 20000, from), me.chatLog.slice(from).join(' | '))
  await s.ask('tag Wrangler remove mobsquad-test-sneak', 200); await sleep(300)
  ;({ from } = await attempt('fox', 'sweet_berries', '{NoAI:1b}'))
  check('a fox won\'t listen unless you crouch', await waitSaid(me, /Crouch first/, 4000, from), me.chatLog.slice(from).join(' | '))

  // the bee: standing still
  ;({ from } = await attempt('bee', 'poppy', '{NoAI:0b}', { at: [px + 2.5, Y + 1, pz + 0.5] }))
  check('a flower starts the bee\'s puzzle', await waitSaid(me, /Stand still/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('standing still for 10 seconds coaxes the bee', await waitSaid(me, /You coaxed a bee/, 15000, from), me.chatLog.slice(from).join(' | '))
  ;({ from } = await attempt('bee', 'poppy', '{NoAI:0b}', { at: [px + 2.5, Y + 1, pz + 0.5] }))
  await waitSaid(me, /Stand still/, 4000, from)
  await s.ask(`tp Wrangler ${px + 3.5} ${Y} ${pz + 0.5}`, 300)
  check('moving starts the bee\'s puzzle over', await waitSaid(me, /You moved/, 4000, from), me.chatLog.slice(from).join(' | '))

  // the allay: three notes from a note block, played by redstone here
  await s.ask(`setblock ${px + 4} ${Y} ${pz - 2} minecraft:note_block`, 300)
  ;({ from } = await attempt('allay', 'amethyst_shard', '{NoAI:1b}'))
  check('an amethyst shard starts the allay\'s puzzle', await waitSaid(me, /note block/, 4000, from), me.chatLog.slice(from).join(' | '))
  for (let i = 0; i < 3; i++) {
    await s.ask(`setblock ${px + 5} ${Y} ${pz - 2} minecraft:redstone_block`, 400)
    await s.ask(`setblock ${px + 5} ${Y} ${pz - 2} minecraft:air`, 400)
  }
  check('three notes coax the allay', await waitSaid(me, /You coaxed an allay/, 4000, from), me.chatLog.slice(from).join(' | '))

  // the axolotl: both in the water
  await s.ask(`fill ${px + 1} ${Y - 3} ${pz - 2} ${px + 5} ${Y - 1} ${pz + 2} minecraft:water`, 400)
  ;({ from } = await attempt('axolotl', 'tropical_fish_bucket', '{NoAI:1b}', { at: [px + 2.5, Y - 2, pz + 0.5], standAt: [px - 1, pz] }))
  check('out of the water, the axolotl won\'t play', await waitSaid(me, /need to be in the water/, 4000, from), me.chatLog.slice(from).join(' | '))
  await s.ask(`tp Wrangler ${px + 1.5} ${Y - 2} ${pz + 0.5} -90 20`, 600)
  from = me.chatLog.length
  await poke(me, entity(me, e => e.name === 'axolotl'))
  check('in the water, a bucket of tropical fish starts it', await waitSaid(me, /wants to play/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('15 seconds in the water coaxes the axolotl', await waitSaid(me, /You coaxed an axolotl/, 20000, from), me.chatLog.slice(from).join(' | '))
  check('the bucket of fish isn\'t used up', me.inventory.items().some(i => i.name === 'tropical_fish_bucket'))
  await s.ask(`fill ${px + 1} ${Y - 3} ${pz - 2} ${px + 5} ${Y - 1} ${pz + 2} minecraft:dirt`, 400)

  for (const k of ['creeper', 'enderman', 'skeleton', 'iron_golem', 'fox', 'bee', 'allay', 'axolotl'])
    check(`a coaxed ${k.replace('_', ' ')} is saved`, count(k) >= 1, file('Wrangler'))
  from = me.chatLog.length
  me.chat('/mobsquad hints')
  check('/mobsquad hints lists every critter', await until(() => me.chatLog.slice(from).filter(l => /^\s+\w/.test(l)).length >= 9, 4000), me.chatLog.slice(from).join(' | '))
}

// ---------- fighting ----------
if (!MENU) skip('fighting')
else if (want('fighting')) {
  section('Fighting hostile mobs only')
  await s.ask('mobsquad xp Wrangler 40000', 300)
  await s.ask('gamemode creative Wrangler', 300) // so hostile mobs leave the player alone and the critters pick the fight
  await s.ask('clear Wrangler', 200)
  await s.ask('gamemode creative Neighbor', 300)
  await stand(other, 'Neighbor', X - 20, Z + 20)
  for (const k of ['iron_golem', 'skeleton', 'allay']) await s.ask(`mobsquad give Wrangler ${k}`, 300) // fresh ones, none resting
  await stand(me, 'Wrangler', X, Z, 0, 20)
  const lines = critterLines('Wrangler')
  const slot = k => lines.map(l => l.includes(' ' + k + ' ')).lastIndexOf(true)
  const fx = X + 4, fz = Z - 4
  await s.ask('kill @e[type=!player]', 400)
  let w = await openBunkhouse(me, vault); await me.clickWindow(slot('iron_golem'), 0, 0); await sleep(300); await closeMenu(me)
  await s.ask(`summon cow ${fx} ${Y} ${fz}`, 200)
  await s.ask(`summon villager ${fx + 1} ${Y} ${fz}`, 200)
  await s.ask(`summon husk ${fx - 2} ${Y} ${fz} {CustomName:"Gerald",PersistenceRequired:1b,NoAI:1b}`, 200)
  await s.ask(`summon enderman ${fx + 2} ${Y} ${fz - 2}`, 200)
  await s.ask(`summon wolf ${fx} ${Y} ${fz + 2} {Owner:[I; ${intIds.Neighbor}]}`, 200)
  await s.ask(`summon husk ${fx + 3} ${Y} ${fz + 1} {Tags:["foe"],PersistenceRequired:1b,NoAI:1b}`, 600) // still, so it can't go after the villager
  const golemDown = await until(async () => await s.count('@e[type=husk,tag=foe]') === 0, 25000, 500)
  check('a golem critter takes down a hostile husk near its owner', golemDown,
    `golems ${await s.count('@e[type=iron_golem]')}, foe health ${await health('@e[type=husk,tag=foe,limit=1]')}, ${(await s.ask('execute as @e[type=iron_golem,limit=1] run data get entity @s Pos', 300)).join(' ').slice(0, 120)}`)
  await sleep(4000)
  const full = async (sel, hp) => (await health(sel)) === hp
  check('it leaves the cow alone', await full('@e[type=cow,limit=1]', 10))
  check('it leaves the villager alone', await full('@e[type=villager,limit=1]', 20))
  check('it leaves a named husk alone unless it starts something', await s.count('@e[type=husk,name=Gerald]') === 1)
  check('it leaves a neutral enderman alone', await full('@e[type=enderman,limit=1]', 40))
  check('it leaves another player\'s pet alone', await full('@e[type=wolf,limit=1]', 8) || await full('@e[type=wolf,limit=1]', 40))
  w = await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300)
  await s.ask('kill @e[type=!player]', 400)
  await me.clickWindow(slot('skeleton'), 0, 0); await sleep(300); await closeMenu(me)
  await stand(other, 'Neighbor', X + 3, Z - 5) // near the fight, beside the line of fire
  await s.ask('gamemode survival Neighbor', 200)
  other.hurt = 0
  await s.ask(`summon husk ${X} ${Y} ${Z - 8} {Tags:["foe"],PersistenceRequired:1b,NoAI:1b}`, 600)
  const shot = await until(async () => await s.count('@e[type=husk,tag=foe]') === 0, 30000, 500)
  check('a skeleton critter shoots a hostile husk down', shot, `skeletons ${await s.count('@e[type=skeleton]')}, foe health ${await health('@e[type=husk,tag=foe,limit=1]')}, hand ${(await s.ask('data get entity @e[type=skeleton,limit=1] equipment.mainhand.id', 300)).join(' ').slice(-40)}`)
  check('its arrows never hurt a player nearby', other.hurt === 0, `health ${other.health}`)
  w = await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300)
  await s.ask('kill @e[type=!player]', 400)
  await me.clickWindow(slot('allay'), 0, 0); await sleep(300); await closeMenu(me)
  await s.ask(`summon husk ${X + 3} ${Y} ${Z - 3} {Tags:["foe"],PersistenceRequired:1b}`, 600)
  await sleep(6000)
  check('the allay never fights', await full('@e[type=husk,tag=foe,limit=1]', 20))
  w = await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300); await closeMenu(me)
  await s.ask('kill @e[type=!player]', 400)
  await s.ask('gamemode survival Wrangler', 300)
}

// ---------- levels and specials ----------
if (!MENU) skip('specials')
else if (want('specials')) {
  section('Levels and specials')
  await s.ask('mobsquad xp Wrangler 40000', 300)
  await s.ask('gamemode survival Wrangler', 200)
  await s.ask('clear Wrangler', 200)
  await s.ask('effect clear Wrangler', 200)
  await s.ask('gamemode creative Neighbor', 200)
  await stand(other, 'Neighbor', X - 20, Z + 20)
  for (const g of ['randomTickSpeed 0', 'random_tick_speed 0']) await s.ask('gamerule ' + g, 150) // only the bee grows crops here
  for (const k of ['chicken', 'axolotl', 'bee', 'fox', 'allay', 'creeper', 'enderman', 'iron_golem']) await s.ask(`mobsquad give Wrangler ${k}`, 250)
  const slotOf = k => critterLines('Wrangler').map(l => l.split(' ')[2] === k).lastIndexOf(true)
  const lineOf = k => critterLines('Wrangler')[slotOf(k)] || ''
  const effects = async sel => (await s.ask(`data get entity ${sel} active_effects`, 400)).join(' ')
  const attr = async (sel, a) => { const m = (await s.ask(`attribute ${sel} minecraft:${a} get`, 400)).join(' ').match(/is ([\d.]+)/); return m ? +m[1] : null }
  async function sendOnly(kind) {
    await stand(me, 'Wrangler', X, Z, 0, 20)
    await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300)
    await s.ask('kill @e[type=!player]', 400)
    await me.clickWindow(slotOf(kind), 0, 0); await sleep(500); await closeMenu(me)
    await until(async () => await s.count(`@e[type=${kind}]`) === 1, 4000, 250)
  }

  // levels and stats
  let out = (await s.ask('mobsquad level Wrangler chicken 50', 400)).join(' ')
  check('an admin can set a critter\'s level', /set \d+ of Wrangler's chickens? to level 50/.test(out), out)
  check('the level and its experience are saved', lineOf('chicken').split(' ').slice(3, 5).join(' ') === '50 24010', lineOf('chicken'))
  await sendOnly('chicken')
  const hp = await attr('@e[type=chicken,limit=1]', 'max_health')
  check('a level 50 chicken has about 3.2 times its health', hp !== null && Math.abs(hp - 4 * (1 + 3 * (1 - (50 / 99) ** 2))) < 0.05, hp)
  check('it comes out at full health', Math.abs((await health('@e[type=chicken,limit=1]')) - hp) < 0.05)
  await sleep(1500)
  check('the chicken: you fall slowly while it\'s out', /slow_falling/.test(await effects('Wrangler')))

  // the axolotl
  await sendOnly('axolotl')
  await sleep(1500)
  const ax = await effects('Wrangler')
  check('the axolotl: water breathing and regeneration near it', /water_breathing/.test(ax) && /regeneration/.test(ax), ax.slice(-200))
  const axHp = await attr('@e[type=axolotl,limit=1]', 'max_health')
  await s.ask(`damage @e[type=axolotl,limit=1] ${Math.round(axHp * 0.7)} minecraft:generic`, 600)
  check('badly hurt, it plays dead and heals', /regeneration/.test(await effects('@e[type=axolotl,limit=1]')))

  // the bee
  const crops = [[1, 1], [-1, 1], [1, -1], [-1, -1], [2, 0], [-2, 0]]
  await sendOnly('bee')
  for (const [dx, dz] of crops) { await s.ask(`setblock ${X + dx} ${Y - 1} ${Z + dz} farmland`, 80); await s.ask(`setblock ${X + dx} ${Y} ${Z + dz} wheat[age=0]`, 80) }
  const grown = async () => { let n = 0; for (const [dx, dz] of crops) if ((await s.ask(`execute if block ${X + dx} ${Y} ${Z + dz} wheat[age=0]`, 120)).join(' ').includes('failed')) n++; return n }
  check('the bee: crops near it grow faster', await until(async () => await grown() >= 2, 15000, 1000), `${await grown()} of ${crops.length} grew`)
  for (const [dx, dz] of crops) { await s.ask(`setblock ${X + dx} ${Y} ${Z + dz} air`, 60); await s.ask(`setblock ${X + dx} ${Y - 1} ${Z + dz} grass_block`, 60) }

  // the fox
  await sendOnly('fox')
  await hold(me, 'Wrangler', 'cobblestone', 8)
  await stand(me, 'Wrangler', X, Z, -90, 0)
  await me.tossStack(me.inventory.items().find(i => i.name === 'cobblestone'))
  await sleep(700)
  await stand(me, 'Wrangler', X - 4, Z, -90, 0)
  const fetched = await until(() => me.inventory.items().some(i => i.name === 'cobblestone'), 20000, 500)
  check('the fox: fetches an item you dropped', fetched)
  await s.ask('gamemode survival Neighbor', 200)
  await stand(other, 'Neighbor', X + 5, Z + 2)
  await hold(other, 'Neighbor', 'oak_log', 4)
  await other.tossStack(other.inventory.items().find(i => i.name === 'oak_log'))
  await stand(other, 'Neighbor', X - 20, Z + 20)
  await sleep(9000)
  check('it leaves other players\' items alone', !me.inventory.items().some(i => i.name === 'oak_log') && await s.count('@e[type=item]') >= 1)
  await s.ask('kill @e[type=item]', 200)

  // the allay
  await sendOnly('allay')
  await s.ask(`setblock ${X + 4} ${Y - 3} ${Z - 2} iron_ore`, 200)
  await hold(me, 'Wrangler', 'raw_iron', 1)
  let from = me.chatLog.length
  await poke(me, entity(me, e => e.name === 'allay'))
  check('the allay: shown an ore, it finds the nearest one', await waitSaid(me, /found iron ore about \d+ blocks away/, 4000, from), me.chatLog.slice(from).join(' | '))
  await sleep(3200)
  await hold(me, 'Wrangler', 'diamond', 1)
  from = me.chatLog.length
  await poke(me, entity(me, e => e.name === 'allay'))
  check('with none close by, it says so', await waitSaid(me, /found no diamond ore close by/, 4000, from), me.chatLog.slice(from).join(' | '))
  await s.ask(`setblock ${X + 4} ${Y - 3} ${Z - 2} stone`, 200)

  // the enderman
  await sendOnly('enderman')
  await s.ask('clear Wrangler', 200)
  const fetchAt = new Vec3(X + 3, Y, Z)
  await s.ask(`setblock ${fetchAt.x} ${fetchAt.y} ${fetchAt.z} pumpkin`, 300)
  async function askFetch() {
    await poke(me, entity(me, e => e.name === 'enderman')); await sleep(500)
    await me.lookAt(fetchAt.offset(0.5, 0.5, 0.5), true); await sleep(300)
    me.swingArm(); await sleep(2000)
  }
  await askFetch()
  check('the enderman: fetches the block you swing at', (await s.ask(`execute if block ${fetchAt.x} ${fetchAt.y} ${fetchAt.z} air`, 200)).join(' ').includes('passed')
    && await until(() => me.inventory.items().some(i => i.name === 'pumpkin'), 3000))
  await s.ask(`setblock ${fetchAt.x} ${fetchAt.y} ${fetchAt.z} stone`, 300)
  from = me.chatLog.length
  await askFetch()
  check('it won\'t carry a block an enderman can\'t', (await s.ask(`execute if block ${fetchAt.x} ${fetchAt.y} ${fetchAt.z} stone`, 200)).join(' ').includes('passed'))
  // on someone else's claim, it won't dig
  await stand(other, 'Neighbor', X + 40, Z - 40)
  await hold(other, 'Neighbor', 'golden_shovel', 1)
  await s.ask('acb Neighbor 400', 300)
  let nfrom = other.chatLog.length
  other.chat('/claim 5')
  const made = await waitSaid(other, /[Cc]laim created|created/, 5000, nfrom)
  await stand(other, 'Neighbor', X - 20, Z + 20)
  const claimed = new Vec3(X + 37, Y, Z - 40)
  await s.ask(`setblock ${claimed.x} ${claimed.y} ${claimed.z} pumpkin`, 300)
  await stand(me, 'Wrangler', X + 33, Z - 40, -90, 0)
  await sleep(1500)
  await poke(me, entity(me, e => e.name === 'enderman')); await sleep(500)
  await me.lookAt(claimed.offset(0.5, 0.5, 0.5), true); await sleep(300)
  me.swingArm(); await sleep(2000)
  if (made) check('it won\'t take a block from someone else\'s claim', (await s.ask(`execute if block ${claimed.x} ${claimed.y} ${claimed.z} pumpkin`, 200)).join(' ').includes('passed'))
  else console.log('  (skipped the claim check: GriefPrevention made no claim)')
  await s.ask(`setblock ${claimed.x} ${claimed.y} ${claimed.z} air`, 200)

  // the creeper
  await sendOnly('creeper')
  await s.ask(`summon cow ${X + 6} ${Y} ${Z - 11}`, 200)
  await s.ask(`summon husk ${X + 3} ${Y} ${Z - 9.5} {Tags:["foe","a"],PersistenceRequired:1b,NoAI:1b}`, 200)
  await s.ask(`summon husk ${X + 4.5} ${Y} ${Z - 9.5} {Tags:["foe","b"],PersistenceRequired:1b,NoAI:1b}`, 600)
  const both = await until(async () => (await health('@e[tag=a,limit=1]') ?? 0) < 20 && (await health('@e[tag=b,limit=1]') ?? 0) < 20, 20000, 400)
  check('the creeper: one blast hurts the hostile mobs around its target', both, `${await health('@e[tag=a,limit=1]')} / ${await health('@e[tag=b,limit=1]')}`)
  check('it leaves the cow alone', (await health('@e[type=cow,limit=1]')) === 10)
  check('it breaks no blocks', (await s.ask(`execute if block ${X + 3} ${Y - 1} ${Z - 10} grass_block`, 200)).join(' ').includes('passed'))
  await s.ask('kill @e[type=husk]', 300)
  await s.ask(`tp Neighbor ${X + 4} ${Y} ${Z - 11}`, 300)
  await s.ask(`summon husk ${X + 3} ${Y} ${Z - 9.5} {Tags:["foe","a"],PersistenceRequired:1b,NoAI:1b}`, 200)
  await s.ask(`summon husk ${X + 4.5} ${Y} ${Z - 9.5} {Tags:["foe","b"],PersistenceRequired:1b,NoAI:1b}`, 300)
  let firstHurt = null
  await until(async () => { const a = await health('@e[tag=a,limit=1]'), b = await health('@e[tag=b,limit=1]'); if (a < 20 || b < 20) { firstHurt = [a, b]; return true } return false }, 15000, 250)
  check('with a player close by it never blasts, only hits one at a time', firstHurt && firstHurt.some(h => h === 20), firstHurt)
  await s.ask(`tp Neighbor ${X - 20} ${Y} ${Z + 20}`, 300)

  // the iron golem: experience, levels and its guard
  await sendOnly('iron_golem')
  await s.ask(`summon husk ${X + 3} ${Y} ${Z - 3} {Tags:["foe"],PersistenceRequired:1b,NoAI:1b}`, 600)
  from = me.chatLog.length
  await until(async () => await s.count('@e[type=husk,tag=foe]') === 0, 20000, 500)
  check('beating a hostile mob earns a critter experience and a level', await waitSaid(me, /Iron golem is now level 2/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('the new level is saved', /^critter \S+ iron_golem 2 20 /.test(lineOf('iron_golem')), lineOf('iron_golem'))
  await s.ask(`summon zombie ${X} ${Y} ${Z + 14} {Tags:["hitter"],PersistenceRequired:1b,NoAI:1b}`, 400)
  await s.ask('effect clear Wrangler', 200)
  await s.ask('effect give Wrangler minecraft:instant_health 1 5', 600)
  const golemBefore = await health('@e[type=iron_golem,limit=1]')
  await s.ask('damage Wrangler 4 minecraft:mob_attack by @e[tag=hitter,limit=1]', 600)
  const meAfter = await health('Wrangler'), golemAfter = await health('@e[type=iron_golem,limit=1]')
  const lost = 20 - meAfter, took = golemBefore - golemAfter
  // easy difficulty makes a 4-point mob hit 3; the golem takes a quarter of it. Natural regen can blur the player's own number.
  check('the iron golem takes a quarter of a hit meant for you', lost > 0 && lost <= 2.26 && Math.abs(took - 0.75) < 0.01, `you lost ${lost}, golem took ${took}`)
  await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300); await closeMenu(me)
  await s.ask('kill @e[type=!player]', 400)
  for (const g of ['randomTickSpeed 3', 'random_tick_speed 3']) await s.ask('gamerule ' + g, 150)
}

// ---------- vittles ----------
if (!MENU) skip('vittles')
else if (want('vittles')) {
  section('Vittles')
  await config({ 'recovery-minutes': 5 })
  await s.ask('clear Wrangler', 200)
  await stand(me, 'Wrangler', X, Z, 0, 20)
  let w = await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300)
  await me.clickWindow(0, 0, 0); await sleep(300); await closeMenu(me)
  const name = critterLines('Wrangler')[0].split(' ')[6]
  let from = me.chatLog.length
  await s.ask(`damage @e[name=${decodeURIComponent(name)},limit=1] 500 minecraft:generic`, 600)
  check('a critter is tuckered out', await waitSaid(me, /tuckered out/, 4000, from))
  await s.ask('mobsquad vittles Wrangler 2', 400)
  await until(() => me.inventory.items().some(i => i.name === 'pumpkin_pie'), 4000)
  await me.equip(me.inventory.items().find(i => i.name === 'pumpkin_pie'), 'hand')
  from = me.chatLog.length
  me.activateItem(); await sleep(300); me.deactivateItem()
  check('vittles perk it up and it comes running', await waitSaid(me, /perked right up and came running/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('its rest is cleared in the file', +critterLines('Wrangler')[0].split(' ')[5] === 0, critterLines('Wrangler')[0])
  check('one bundle is used', me.inventory.items().find(i => i.name === 'pumpkin_pie')?.count === 1)
  const stillTired = critterLines('Wrangler').some(l => +l.split(' ')[5] > Date.now())
  from = me.chatLog.length
  me.activateItem(); await sleep(300); me.deactivateItem()
  if (stillTired) check('a second bundle perks up the next tired critter', await waitSaid(me, /perked right up/, 4000, from), me.chatLog.slice(from).join(' | '))
  from = me.chatLog.length
  const left = me.inventory.items().find(i => i.name === 'pumpkin_pie')?.count ?? 0
  if (left) { me.activateItem(); await sleep(300); me.deactivateItem() }
  else { await s.ask('mobsquad vittles Wrangler 1', 400); await until(() => me.inventory.items().some(i => i.name === 'pumpkin_pie'), 4000); await me.equip(me.inventory.items().find(i => i.name === 'pumpkin_pie'), 'hand'); me.activateItem(); await sleep(300); me.deactivateItem() }
  check('with nobody tuckered out, vittles say so and stay', await waitSaid(me, /needs vittles/, 4000, from) && (me.inventory.items().find(i => i.name === 'pumpkin_pie')?.count ?? 0) >= 1, me.chatLog.slice(from).join(' | '))
  w = await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300); await closeMenu(me)
  await config({ 'recovery-minutes': 0 })
}

// ---------- leaving, restarts and crashes ----------
if (!MENU) skip('persist')
else if (want('persist')) {
  section('Leaving and restarts')
  await s.ask('mobsquad xp Wrangler 15000', 300)
  await s.ask('clear Wrangler', 200)
  await stand(me, 'Wrangler', X, Z, 0, 20)
  let w = await openBunkhouse(me, vault)
  if (!(await s.count('@e[name=Chicken]'))) { await me.clickWindow(0, 0, 0); await sleep(300) }
  await closeMenu(me)
  const count = critterLines('Wrangler').length
  check('a critter is out before leaving', await s.count('@e[type=chicken,name=Chicken]') >= 1)
  await quit(me)
  await sleep(1000)
  check('leaving takes critters out of the world', await s.count('@e[type=chicken,name=Chicken]') === 0)
  me = await bot('Wrangler', { physics: PHYSICS })
  await sleep(1000)
  check('coming back, the Bunkhouse list is whole', critterLines('Wrangler').length === count, `${critterLines('Wrangler').length} of ${count}`)
  check('coming back, nothing is out', await s.count('@e[type=chicken,name=Chicken]') === 0)

  await stand(me, 'Wrangler', X, Z, 0, 20)
  w = await openBunkhouse(me, vault); await me.clickWindow(0, 0, 0); await sleep(300); await closeMenu(me)
  await quit(me); await quit(other)
  await s.stop()
  await s.start()
  check('after a restart no critter is in the world', await s.count('@e[name=Chicken]') === 0)
  check('after a restart the list is whole', critterLines('Wrangler').length === count)
  me = await bot('Wrangler', { physics: PHYSICS })
  await stand(me, 'Wrangler', X, Z, 0, 20)
  w = await openBunkhouse(me, vault)
  check('the Bunkhouse still opens after a restart', egg(w, 0) === 'chicken_spawn_egg', egg(w, 0))
  await me.clickWindow(0, 0, 0); await sleep(500); await closeMenu(me)
  check('a critter is out before the crash', await s.count('@e[type=chicken,name=Chicken]') === 1)
  await s.kill9()
  await s.start()
  me = await bot('Wrangler', { physics: PHYSICS })
  await stand(me, 'Wrangler', X, Z, 0, 20)
  await sleep(2000)
  check('after a crash the critter isn\'t left in the world', await s.count('@e[type=chicken]') === 0)
  check('after a crash the list is whole, with no copy', critterLines('Wrangler').length === count)
}


// ---------- worlds where MobSquad is off ----------
if (!MENU) skip('worlds')
else if (want('worlds')) {
  section('Disabled worlds')
  await config({ 'disabled-worlds': '[testworld]', 'recovery-minutes': 0 })
  await stand(me, 'Wrangler', X, Z, 0, 20)
  const from = me.chatLog.length
  await me.activateBlock(me.blockAt(vault))
  check('a Bunkhouse in a disabled world says MobSquad is off', await waitSaid(me, /off in this world/, 4000, from), me.chatLog.slice(from).join(' | '))
  await config({ 'recovery-minutes': 0 })
}

const errors = s.exceptions()
check('no MobSquad errors in the log', errors.length === 0, errors.slice(0, 5).join('\n'))
await quit(me); await quit(other)
await s.stop()
process.exit(summary() ? 1 : 0)
