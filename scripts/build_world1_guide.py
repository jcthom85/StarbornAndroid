import os
import sys
from PIL import Image
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image as RLImage, PageBreak, KeepTogether, HRFlowable
)

def build_world1_pdf(output_pdf):
    doc = SimpleDocTemplate(
        output_pdf,
        pagesize=letter,
        leftMargin=36,
        rightMargin=36,
        topMargin=36,
        bottomMargin=36
    )

    styles = getSampleStyleSheet()
    
    # Custom Palette
    c_primary = colors.HexColor(#0D1B2A)
    c_accent = colors.HexColor(#00B4D8)
    c_gold = colors.HexColor(#FFB703)
    c_dark = colors.HexColor(#1B263B)
    c_light = colors.HexColor(#F8F9FA)
    c_danger = colors.HexColor(#E63946)

    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontName='Helvetica-Bold',
        fontSize=24,
        leading=28,
        textColor=c_accent,
        alignment=1
    )
    subtitle_style = ParagraphStyle(
        'DocSubTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=12,
        leading=16,
        textColor=c_gold,
        alignment=1
    )
    h1_style = ParagraphStyle(
        'SectionH1',
        parent=styles['Heading1'],
        fontName='Helvetica-Bold',
        fontSize=15,
        leading=18,
        textColor=colors.HexColor(#0077B6),
        spaceBefore=12,
        spaceAfter=6
    )
    h2_style = ParagraphStyle(
        'SectionH2',
        parent=styles['Heading2'],
        fontName='Helvetica-Bold',
        fontSize=12,
        leading=15,
        textColor=colors.HexColor(#1D3557),
        spaceBefore=8,
        spaceAfter=4
    )
    body_style = ParagraphStyle(
        'BodyDark',
        parent=styles['BodyText'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=13,
        textColor=colors.HexColor(#2B2D42)
    )
    body_bold = ParagraphStyle(
        'BodyBold',
        parent=body_style,
        fontName='Helvetica-Bold'
    )
    callout_style = ParagraphStyle(
        'Callout',
        parent=body_style,
        fontName='Helvetica-Oblique',
        textColor=colors.HexColor(#1D3557)
    )

    story = []

    # --- COVER / HEADER ---
    story.append(Paragraph(STARBORN: OFFICIAL PLAYTESTER FIELD MANUAL, title_style))
    story.append(Paragraph(VOLUME 1: WORLD 1 — THE PIT & THE GREAT ESCAPE, subtitle_style))
    story.append(Spacer(1, 10))
    story.append(HRFlowable(width=100%, thickness=2, color=c_accent, spaceBefore=4, spaceAfter=12))

    # Hero Visual Banner
    banner_path = world_assets/src/main/assets/images/hubs/homestead.webp
    if os.path.exists(banner_path):
        img = Image.open(banner_path)
        png_temp = homestead_banner.png
        img.save(png_temp)
        rl_img = RLImage(png_temp, width=520, height=140)
        story.append(rl_img)
        story.append(Spacer(1, 10))

    # Overview Table
    overview_data = [
        [Paragraph(<b>World</b>: 1 (Homestead Colony), body_style), Paragraph(<b>Quests</b>: w1_mq01 to w1_mq05 (5 Main), body_style)],
        [Paragraph(<b>Primary Relic</b>: The Echo (Tuning Fork), body_style), Paragraph(<b>Party Recruits</b>: Nova (Lead), Zeke (Contact), body_style)],
        [Paragraph(<b>Key Boss</b>: The Iron Warden (Launch Bay), body_style), Paragraph(<b>Target Clear Time</b>: 25 - 35 Minutes, body_style)],
    ]
    t_overview = Table(overview_data, colWidths=[260, 260])
    t_overview.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor(#E0F2FE)),
        ('BOX', (0,0), (-1,-1), 1, c_accent),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor(#BAE6FD)),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
    ]))
    story.append(t_overview)
    story.append(Spacer(1, 12))

    # --- QUEST BREAKDOWNS ---
    
    # CHAPTER 1
    story.append(Paragraph(Chapter 1: Wake in the Pit (Quest: w1_mq01), h1_style))
    story.append(Paragraph(<b>Narrative Hook:</b> Nova awakens in the lower barracks. Her mining cutter's cryo-inductor is cracked and quota is due at shift call., body_style))
    story.append(Spacer(1, 4))
    
    ch1_steps = [
        [Paragraph(<b>Step</b>, body_bold), Paragraph(<b>Action & Location</b>, body_bold), Paragraph(<b>Details & Tactical Notes</b>, body_bold)],
        [Paragraph(1.1, body_style), Paragraph(<b>Turn on Bunk Light</b><br/>Room: <i>pit_nova_bunk</i>, body_style), Paragraph(Tap the inline action <b>'Bunk Light'</b> to illuminate Nova's cabin and reveal interactables., body_style)],
        [Paragraph(1.2, body_style), Paragraph(<b>Isolate Conduit</b><br/>Room: <i>pit_nova_bunk</i>, body_style), Paragraph(Tap <b>'Scorched Conduit'</b> to clear the safety fault flag., body_style)],
        [Paragraph(1.3, body_style), Paragraph(<b>Report to Jed</b><br/>Room: <i>pit_jed_bunk</i>, body_style), Paragraph(Navigate: <b>North &rarr; East</b>. Talk to Jed to receive starter rations & kit., body_style)],
        [Paragraph(1.4, body_style), Paragraph(<b>Workshop Combat</b><br/>Room: <i>workshop_yard</i>, body_style), Paragraph(Navigate: <b>North</b>. Inspect <i>Loader Relay</i>.<br/><b>Combat: Faulted Loader</b> (Weakness: <b>Shock</b>). Use Shock to stagger., body_style)],
        [Paragraph(1.5, body_style), Paragraph(<b>Tinker & Test</b><br/>Room: <i>workshop_floor</i>, body_style), Paragraph(1. Field Menu &rarr; <b>Tinker</b> &rarr; Craft <i>Functional Cryo-Inductor</i>.<br/>2. Inspect <i>Flux Liner</i> & ground it.<br/>3. Confirm <i>Governor Bypass</i> & fire <i>Live Cutter Test</i>., body_style)],
    ]
    t_ch1 = Table(ch1_steps, colWidths=[35, 185, 300])
    t_ch1.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor(#0077B6)),
        ('TEXTCOLOR', (0,0), (-1,0), colors.white),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor(#0077B6)),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor(#E2E8F0)),
        ('TOPPADDING', (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
    ]))
    story.append(t_ch1)
    story.append(Spacer(1, 10))

    # CHAPTER 2
    story.append(Paragraph(Chapter 2: Shift Clearance (Quest: w1_mq02), h1_style))
    story.append(Paragraph(<b>Narrative Hook:</b> The cutter test tripped a power surge. Nova must clear her flagged operator badge before Dominion marks her for bio-reclamation., body_style))
    story.append(Spacer(1, 4))
    
    ch2_steps = [
        [Paragraph(<b>Step</b>, body_bold), Paragraph(<b>Action & Location</b>, body_bold), Paragraph(<b>Details & Tactical Notes</b>, body_bold)],
        [Paragraph(2.1, body_style), Paragraph(<b>Approach Gate</b><br/>Room: <i>checkpoint_queue</i>, body_style), Paragraph(Navigate: <b>North</b> through Market. Talk to <b>Guard Hank</b> (scanner denies access)., body_style)],
        [Paragraph(2.2, body_style), Paragraph(<b>Consult Zeke</b><br/>Room: <i>checkpoint_booth</i>, body_style), Paragraph(Navigate: <b>East</b>. Speak to Zeke at Admin Window.<br/>Select choice: <b>'Grid Instability'</b>. Zeke clears badge liability., body_style)],
    ]
    t_ch2 = Table(ch2_steps, colWidths=[35, 185, 300])
    t_ch2.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor(#0077B6)),
        ('TEXTCOLOR', (0,0), (-1,0), colors.white),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor(#0077B6)),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor(#E2E8F0)),
        ('TOPPADDING', (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
    ]))
    story.append(t_ch2)
    story.append(Spacer(1, 10))

    # CHAPTER 3
    story.append(Paragraph(Chapter 3: Heavy Lifting & The Echo Relic (Quest: w1_mq03), h1_style))
    story.append(Paragraph(<b>Narrative Hook:</b> Pass security certification, descend the deep elevator into the forbidden mines, and touch the resonant artifact., body_style))
    story.append(Spacer(1, 4))

    ch3_steps = [
        [Paragraph(<b>Step</b>, body_bold), Paragraph(<b>Action & Location</b>, body_bold), Paragraph(<b>Details & Tactical Notes</b>, body_bold)],
        [Paragraph(3.1, body_style), Paragraph(<b>Foreman Review</b><br/>Room: <i>admin_lobby</i>, body_style), Paragraph(Talk to <b>Foreman Boggs</b>. Directs you east to pass shield drill training., body_style)],
        [Paragraph(3.2, body_style), Paragraph(<b>Shield Drill</b><br/>Room: <i>workshop_dock</i>, body_style), Paragraph(<b>Combat: Shield Trainer</b> (Acoustic Bulwark). Use heavy physical attacks to shatter barrier. Return to Boggs to log cert., body_style)],
        [Paragraph(3.3, body_style), Paragraph(<b>Deep Mine Descent</b><br/>Room: <i>mine_landing</i>, body_style), Paragraph(Take Deep Lift down.<br/><b>Combat: Echo-Borer</b> (Weakness: <b>Thermal / Burn</b>, Resists Shock!)., body_style)],
        [Paragraph(3.4, body_style), Paragraph(<b>The Echo Relic</b><br/>Room: <i>echo_gap</i>, body_style), Paragraph(Navigate: <b>South</b>. Touch <b>The Echo Relic</b> (Tuning Fork) &rarr; Triggers relic sync cutscene., body_style)],
    ]
    t_ch3 = Table(ch3_steps, colWidths=[35, 185, 300])
    t_ch3.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor(#0077B6)),
        ('TEXTCOLOR', (0,0), (-1,0), colors.white),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor(#0077B6)),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor(#E2E8F0)),
        ('TOPPADDING', (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
    ]))
    story.append(t_ch3)
    story.append(Spacer(1, 10))

    # CHAPTER 4 & 5 - CLIMAX
    story.append(Paragraph(Chapter 4 & 5: Lockdown Escape & The Iron Warden (Quests: w1_mq04 - w1_mq05), h1_style))
    story.append(Paragraph(<b>Narrative Hook:</b> Dominion triggers full colony lockdown. Jed stays behind to hold the lift while Nova & Zeke fight to the escape pod., body_style))
    story.append(Spacer(1, 4))

    ch4_steps = [
        [Paragraph(<b>Step</b>, body_bold), Paragraph(<b>Action & Location</b>, body_bold), Paragraph(<b>Details & Tactical Notes</b>, body_bold)],
        [Paragraph(4.1, body_style), Paragraph(<b>Lift Defense</b><br/>Room: <i>launch_lift</i>, body_style), Paragraph(<b>Combat: Acoustic Bulwark + Resonance Buoy</b>.<br/><b>CRITICAL:</b> Kill the <i>Resonance Buoy</i> on Turn 1 before it shields the Bulwark! Talk to Jed., body_style)],
        [Paragraph(5.1, body_style), Paragraph(<b>BOSS: The Iron Warden</b><br/>Room: <i>launch_bay</i>, body_style), Paragraph(<b>Boss Stats:</b> HP 260 | Stability 65 | Weakness: <b>Shock</b>.<br/>&bull; <b>Round 1-2:</b> Use Shock Arc to shred stability.<br/>&bull; <b>Round 3:</b> Warden charges <i>Overload Cleave</i> &rarr; <b>SELECT DEFEND</b>!<br/>&bull; <b>Round 4+:</b> Break stability to stagger and finish., body_style)],
        [Paragraph(5.2, body_style), Paragraph(<b>Pod Launch</b><br/>Room: <i>launch_bay</i>, body_style), Paragraph(Talk to Zeke twice. Splice <i>Ghost Signal Cell</i> into console. Tap <b>'Nav Console'</b> &rarr; Escape to World 2!, body_style)],
    ]
    t_ch4 = Table(ch4_steps, colWidths=[35, 185, 300])
    t_ch4.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), colors.HexColor(#E63946)),
        ('TEXTCOLOR', (0,0), (-1,0), colors.white),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor(#E63946)),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor(#E2E8F0)),
        ('TOPPADDING', (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
    ]))
    story.append(t_ch4)
    story.append(Spacer(1, 14))

    # Tactical Callout Box
    tips_data = [[
        Paragraph(<b>PRO-TIP: World 1 Speed & Safety Checklist</b><br/>
                  1. <b>Equip Rations:</b> Ensure Nova equips <i>Ration Pack</i> in her Snack slot for 0-cost instant heals.<br/>
                  2. <b>Don't Shock the Echo-Borer:</b> It resists Shock by +50%. Use basic physical attacks if you don't have Thermal skills.<br/>
                  3. <b>Round 3 Boss Defend:</b> The Iron Warden's cleave deals 45+ unblocked damage. Guarding reduces damage by 70%., callout_style)
    ]]
    t_tips = Table(tips_data, colWidths=[520])
    t_tips.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor(#FEF3C7)),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor(#F59E0B)),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
    ]))
    story.append(t_tips)

    doc.build(story)
    print(fWorld 1 Guide built successfully: {output_pdf})

if __name__ == '__main__':
    out_dir = docs/playtest_guides
    os.makedirs(out_dir, exist_ok=True)
    out_file = os.path.join(out_dir, STARBORN_PLAYTEST_GUIDE_WORLD_1.pdf)
    build_world1_pdf(out_file)
