"""Build the World 2 PDF from the maintained Markdown walkthrough."""
from pathlib import Path
import re
from html import escape
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, KeepTogether
from reportlab.platypus.tableofcontents import TableOfContents

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs/playtest/walkthroughs/WORLD_2_COMPLETIONIST_WALKTHROUGH.md"
OUTPUT = ROOT / "docs/playtest_guides/STARBORN_PLAYTEST_GUIDE_WORLD_2.pdf"

class GuideDocument(SimpleDocTemplate):
    def beforeDocument(self):
        self.seq.reset("heading", 0)

    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph) and flowable.style.name in ("Section", "Node"):
            level = 0 if flowable.style.name == "Section" else 1
            key = "heading-" + str(self.seq.nextf("heading"))
            title = flowable.getPlainText()
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(title, key, level=level, closed=True)
            self.notify("TOCEntry", (level, title, self.page, key))

def inline(text):
    text = escape(text)
    text = re.sub(r"`([^`]+)`", r'<font name="Courier" size="8">\1</font>', text)
    text = re.sub(r"\*\*(.+?)\*\*", r'<b>\1</b>', text)
    return text

def build_world2_pdf(output_pdf=OUTPUT):
    normal = Path("C:/Windows/Fonts/segoeui.ttf")
    bold = Path("C:/Windows/Fonts/segoeuib.ttf")
    if normal.exists() and bold.exists():
        pdfmetrics.registerFont(TTFont("Guide", str(normal)))
        pdfmetrics.registerFont(TTFont("Guide-Bold", str(bold)))
        pdfmetrics.registerFontFamily("Guide", normal="Guide", bold="Guide-Bold", italic="Guide", boldItalic="Guide-Bold")
        font, heavy = "Guide", "Guide-Bold"
    else:
        font, heavy = "Helvetica", "Helvetica-Bold"
    styles = {
        "Title": ParagraphStyle("Title", fontName=heavy, fontSize=25, leading=30, textColor=colors.HexColor("#073844"), spaceAfter=20),
        "Body": ParagraphStyle("Body", fontName=font, fontSize=10, leading=14, spaceAfter=7),
        "Step": ParagraphStyle("Step", fontName=font, fontSize=10, leading=14, spaceAfter=9, leftIndent=15, firstLineIndent=-15),
        "Bullet": ParagraphStyle("Bullet", fontName=font, fontSize=9.5, leading=13, spaceAfter=7, leftIndent=11, firstLineIndent=-11),
        "Section": ParagraphStyle("Section", fontName=heavy, fontSize=17, leading=22, spaceAfter=14, textColor=colors.HexColor("#073844"), keepWithNext=True),
        "Node": ParagraphStyle("Node", fontName=heavy, fontSize=12, leading=16, spaceBefore=12, spaceAfter=9, textColor=colors.HexColor("#075c70"), keepWithNext=True),
        "Room": ParagraphStyle("Room", fontName=heavy, fontSize=11, leading=15, spaceBefore=12, spaceAfter=8, keepWithNext=True),
    }
    story = []
    lines = SOURCE.read_text(encoding="utf-8").splitlines()
    story.append(Paragraph(inline(lines[0].lstrip("# ")), styles["Title"]))
    story.append(Paragraph("Detailed route, puzzle solutions, side quests and all-room reference", styles["Body"]))
    story.append(Paragraph("Reviewed October 2, 2026 | Version 1.3.84 (168)<br/>Full spoilers | Repository review; device playtesting remains necessary", styles["Body"]))
    story.append(Spacer(1, 18))
    story.append(Paragraph("Keep this open while playing. Follow the route in order and stop at each completion check. Use the contents and PDF bookmarks to jump to your current room or quest.", styles["Body"]))
    story.append(PageBreak())
    story.append(Paragraph("Contents", styles["Title"]))
    toc = TableOfContents()
    toc.levelStyles = [ParagraphStyle("TOC0", fontName=heavy, fontSize=10, leading=14, spaceBefore=6),
                       ParagraphStyle("TOC1", fontName=font, fontSize=9, leading=12, leftIndent=14)]
    story.append(toc)
    story.append(PageBreak())
    first_section = next(i for i, line in enumerate(lines) if line.startswith("## "))
    for line in lines[first_section:]:
        if not line.strip(): continue
        if line.startswith("## "):
            if not isinstance(story[-1], PageBreak): story.append(PageBreak())
            story.append(Paragraph(inline(line[3:]), styles["Section"]))
        elif line.startswith("### "): story.append(Paragraph(inline(line[4:]), styles["Node"]))
        elif line.startswith("#### "): story.append(Paragraph(inline(line[5:]), styles["Room"]))
        elif line.startswith("- "): story.append(Paragraph(inline(line[2:]), styles["Bullet"], bulletText="-"))
        elif re.match(r"^\d+\. ", line): story.append(Paragraph(inline(line), styles["Step"]))
        else: story.append(Paragraph(inline(line), styles["Body"]))
    def footer(canvas, doc):
        canvas.saveState()
        canvas.setStrokeColor(colors.HexColor("#b6c9cf"))
        canvas.line(40, 36, 572, 36)
        canvas.setFont(font, 8)
        canvas.setFillColor(colors.HexColor("#44616b"))
        canvas.drawString(40, 24, "STARBORN | World 2 walkthrough | 1.3.84 | October 2, 2026")
        canvas.drawRightString(572, 24, str(doc.page))
        canvas.restoreState()
    output = Path(output_pdf)
    output.parent.mkdir(parents=True, exist_ok=True)
    doc = GuideDocument(str(output), pagesize=letter, leftMargin=40, rightMargin=40,
                        topMargin=40, bottomMargin=49, title="Starborn World 2 Complete Walkthrough",
                        author="Starborn development")
    doc.multiBuild(story, onFirstPage=footer, onLaterPages=footer)
    print(output)

if __name__ == "__main__":
    build_world2_pdf()
